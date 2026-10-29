#!/bin/bash
# End-to-end auth + ownership test. Usage: scripts/auth_test.sh [base-url]   (default http://localhost:8080)
# Creates two throwaway accounts on whatever server you point it at, so only run it against production deliberately.
# Written for macOS bash 3.2: JSON bodies come from printf helpers, never from nested escaped quotes.
A=${1:-http://localhost:8080}/api
j='Content-Type: application/json'
S=$RANDOM$RANDOM
EA="alice$S@example.com"; EB="bob$S@example.com"; EAU=$(echo "$EA" | tr a-z A-Z)
pass=0; fail=0

reg()  { printf '{"email":"%s","password":"%s","recoveryQuestion":"%s","recoveryAnswer":"%s"}' "$1" "$2" "$3" "$4"; }
cred() { printf '{"email":"%s","password":"%s"}' "$1" "$2"; }
rst()  { printf '{"email":"%s","recoveryAnswer":"%s","newPassword":"%s"}' "$1" "$2" "$3"; }
baby() { printf '{"name":"%s","birthdate":"2026-06-01"}' "$1"; }
prog() { printf '{"checklistItemId":"%s","completed":%s}' "$1" "$2"; }

code() { curl -s -o /tmp/body -w '%{http_code}' "$@"; }
post() { # path body [token]
  if [ -n "$3" ]; then code -X POST "$A$1" -H "$j" -H "Authorization: Bearer $3" -d "$2"
  else code -X POST "$A$1" -H "$j" -d "$2"; fi
}
get()  { if [ -n "$2" ]; then code -H "Authorization: Bearer $2" "$A$1"; else code "$A$1"; fi; }
del()  { code -X DELETE -H "Authorization: Bearer $2" "$A$1"; }
tok()  { sed 's/.*"token":"\([^"]*\)".*/\1/'; }
check() { # name expected actual
  if [ "$2" = "$3" ]; then pass=$((pass+1)); printf 'PASS %-46s %s\n' "$1" "$3"; else fail=$((fail+1)); printf 'FAIL %-46s expected [%s] got [%s]  %s\n' "$1" "$2" "$3" "$(head -c 160 /tmp/body)"; fi
}
contains() { grep -c "$1" /tmp/body; }

check "no token -> /babies"                401 "$(get /babies)"
check "garbage token -> /babies"           401 "$(get /babies abc)"
check "weak password"                      400 "$(post /auth/register "$(reg w$S@x.com short q a)")"
check "password over 72 bytes"             400 "$(post /auth/register "$(reg l$S@x.com $(printf 'a%.0s' $(seq 1 73)) q a)")"
check "bad email"                          400 "$(post /auth/register "$(reg nope password123 q a)")"
check "blank recovery answer"              400 "$(post /auth/register "$(reg m$S@x.com password123 q ' ')")"
check "malformed JSON is a 400, not a 500" 400 "$(post /auth/register '{not json')"

TA=$(curl -s -X POST $A/auth/register -H "$j" -d "$(reg " $EAU " password123 'First pet?' Rex)" | tok)
TB=$(curl -s -X POST $A/auth/register -H "$j" -d "$(reg "$EB" password123 'First pet?' Fido)" | tok)
check "register A (email trimmed+lowercased)" 1 "$([ ${#TA} -gt 100 ] && echo 1 || echo 0)"
check "register B"                         1 "$([ ${#TB} -gt 100 ] && echo 1 || echo 0)"
check "duplicate email, different case"    409 "$(post /auth/register "$(reg "$EAU" password123 q a)")"
R=$(get /auth/me "$TA")
check "/me returns normalized email"       "200 {\"email\":\"$EA\"}" "$R $(cat /tmp/body)"

BID=$(curl -s -X POST $A/babies -H "Authorization: Bearer $TA" -H "$j" -d "$(baby 'Alice baby')" | sed 's/.*"id":"\([^"]*\)".*/\1/')
check "A creates baby"                     1 "$([ ${#BID} -eq 36 ] && echo 1 || echo 0)"
check "blank baby name rejected"           400 "$(post /babies "$(baby '  ')" "$TA")"
check "A lists own baby"                   1 "$(get /babies "$TA" >/dev/null; contains "$BID")"
R=$(get /babies "$TB")
check "B lists nothing"                    "200 []" "$R $(cat /tmp/body)"
check "B GET A's baby"                     404 "$(get /babies/$BID "$TB")"
check "B GET A's progress"                 404 "$(get /babies/$BID/progress "$TB")"
check "B WRITE A's progress"               404 "$(post /babies/$BID/progress "$(prog 0-s1 true)" "$TB")"
check "B DELETE A's baby"                  404 "$(del /babies/$BID "$TB")"
check "A's baby survived B's delete"       200 "$(get /babies/$BID "$TA")"
check "A checks an item"                   200 "$(post /babies/$BID/progress "$(prog 0-s1 true)" "$TA")"
check "A progress has the item"            1 "$(get /babies/$BID/progress "$TA" >/dev/null; contains '"checklistItemId":"0-s1"')"
check "A unchecks the item"                200 "$(post /babies/$BID/progress "$(prog 0-s1 false)" "$TA")"
check "A progress is empty again"          "[]" "$(get /babies/$BID/progress "$TA" >/dev/null; cat /tmp/body)"
check "A deletes own baby"                 200 "$(del /babies/$BID "$TA")"
check "deleted baby is gone (404)"         404 "$(get /babies/$BID "$TA")"

check "login ok"                           200 "$(post /auth/login "$(cred "$EA" password123)")"
check "login ok with different email case" 200 "$(post /auth/login "$(cred "$EAU" password123)")"
check "login wrong password"               401 "$(post /auth/login "$(cred "$EA" wrongpass1)")"
check "login unknown email"                401 "$(post /auth/login "$(cred ghost@example.com wrongpass1)")"
for i in 1 2 3 4; do post /auth/login "$(cred "$EB" bad-pass-1)" >/dev/null; done
check "5th wrong password still 401"       401 "$(post /auth/login "$(cred "$EB" bad-pass-1)")"
check "locked out after 5 failures (429)"  429 "$(post /auth/login "$(cred "$EB" password123)")"
check "lockout is per account (A unaffected)" 200 "$(post /auth/login "$(cred "$EA" password123)")"

R=$(get "/auth/recovery-question?email=$EA")
check "recovery question"                  "200 {\"question\":\"First pet?\"}" "$R $(cat /tmp/body)"
check "recovery unknown email"             404 "$(get '/auth/recovery-question?email=ghost@example.com')"
check "reset with wrong answer"            401 "$(post /auth/reset-password "$(rst "$EA" cat newpassword1)")"
check "reset with weak new password"       400 "$(post /auth/reset-password "$(rst "$EA" rex short)")"
check "reset with right answer (case/space)" 204 "$(post /auth/reset-password "$(rst "$EA" '  REX ' newpassword1)")"
check "login with new password"            200 "$(post /auth/login "$(cred "$EA" newpassword1)")"
check "old password rejected"              401 "$(post /auth/login "$(cred "$EA" password123)")"

check "CORS: allowed origin preflight"     200 "$(curl -s -o /dev/null -w '%{http_code}' -X OPTIONS $A/babies -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: GET' -H 'Access-Control-Request-Headers: authorization,content-type')"
check "CORS: ACAO echoes the origin"       "http://localhost:5173" "$(curl -s -i -X OPTIONS $A/babies -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: GET' | tr -d '\r' | grep -i '^access-control-allow-origin' | cut -d' ' -f2)"
check "CORS: foreign origin rejected"      403 "$(curl -s -o /dev/null -w '%{http_code}' -X OPTIONS $A/babies -H 'Origin: http://evil.example' -H 'Access-Control-Request-Method: GET')"

echo; echo "passed=$pass failed=$fail"
[ $fail -eq 0 ]
