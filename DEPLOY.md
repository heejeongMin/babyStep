# Deploying Baby Care Guide to the shared VM

Same layout as wishjar and sideband: one compose stack per app on the Hetzner VM, with `edge-caddy` as the only
public 80/443 listener. Public URL: `https://babysteps.5.223.79.124.sslip.io` (no DNS setup needed; to use a
DuckDNS name instead, change the site label in `edge-caddy/Caddyfile` and `CORS_ALLOWED_ORIGINS`).

What is verified locally (nothing here has been run on the VM yet):
- `docker-compose.prod.yml` builds and starts cleanly (0 errors, 0 restarts), schema + Liquibase migrate, app uses
  ~290 MB of its 640 MB limit, and the 43-check auth/ownership test passes against it.
- The new `Caddyfile` validates, and routing (`/`, `/privacy`, SPA fallback, `/api` proxy) works against the built files.

## 0. Check the VM first (read-only)

```bash
ssh -i ~/.ssh/hetzner_dogrescue root@5.223.79.124 '
  uname -m; free -m; swapon --show; df -h / | tail -1
  docker stats --no-stream --format "{{.Name}} {{.MemUsage}}"
  docker inspect edge-caddy --format "{{range .Mounts}}{{.Source}} -> {{.Destination}}{{println}}{{end}}"
'
```

- `uname -m` decides how to build the image (see step 2).
- If free memory is tight, STOP: this adds one JVM (~300 MB) and one Postgres (~60 MB) to a VM that has had OOM
  problems before.
- The `edge-caddy` mount sources give the real `EDGE_DIR` used below.

## 1. Frontend (static files)

```bash
cd BabyStepsGuide && npm run build:web            # Node 20; output in dist/public
rsync -az --delete -e "ssh -i ~/.ssh/hetzner_dogrescue" dist/public/ root@5.223.79.124:$EDGE_DIR/babysteps-web/
scp -i ~/.ssh/hetzner_dogrescue edge-caddy/babysteps-static/privacy.html root@5.223.79.124:$EDGE_DIR/babysteps-static/
```

## 2. Backend

On the VM, create `/root/baby-steps/.env` from `.env.example` with a real `POSTGRES_PASSWORD` and
`JWT_SECRET` (`openssl rand -base64 48`). Never commit it.

Option A, build on the VM (what the sibling projects do; a Gradle build needs memory, so check step 0):

```bash
rsync -az --exclude build --exclude .gradle --exclude .git -e "ssh -i ~/.ssh/hetzner_dogrescue" baby-steps/ root@5.223.79.124:/root/baby-steps/
ssh ... 'cd /root/baby-steps && docker compose -f docker-compose.prod.yml up -d --build'
```

Option B, build locally for the VM's architecture and ship the image (safer on a small VM):

```bash
docker buildx build --platform linux/amd64 -t baby-steps-app:latest --load baby-steps   # arm64 VM: linux/arm64
docker save baby-steps-app:latest | ssh ... docker load
# then use `image: baby-steps-app:latest` in place of `build: .` in the compose file on the VM
```

## 3. Edge proxy (this touches the shared Caddy that serves wishjar and sideband)

```bash
# on the VM, in $EDGE_DIR
cp Caddyfile Caddyfile.bak.$(date +%F) && cp docker-compose.yml docker-compose.yml.bak.$(date +%F)
# copy over the updated Caddyfile and docker-compose.yml, then:
docker exec edge-caddy caddy validate --config /etc/caddy/Caddyfile     # must say "Valid configuration"
docker compose up -d        # recreates edge-caddy to pick up the two new volume mounts (brief blip for all sites)
```

## 4. Smoke test

```bash
curl -sI https://babysteps.5.223.79.124.sslip.io/ | head -1            # 200
curl -s  https://babysteps.5.223.79.124.sslip.io/privacy | head -c 200
curl -s -o /dev/null -w '%{http_code}\n' https://babysteps.5.223.79.124.sslip.io/api/babies        # 401 (needs login)
curl -s -o /dev/null -w '%{http_code}\n' https://babysteps.5.223.79.124.sslip.io/actuator/health   # NOT 200 (not proxied)
scripts/auth_test.sh https://babysteps.5.223.79.124.sslip.io     # creates 2 throwaway accounts; its CORS checks use localhost:5173, so 2 of 43 will differ
```

Also confirm wishjar and sideband still answer after the Caddy change.

## Rollback

```bash
cd $EDGE_DIR && cp Caddyfile.bak.<date> Caddyfile && cp docker-compose.yml.bak.<date> docker-compose.yml && docker compose up -d
cd /root/baby-steps && docker compose -f docker-compose.prod.yml down        # add -v to also delete the database
```

## Known limits at launch

- Password reset uses a recovery question (no email service); it is the weakest part of auth.
- Login token is kept in `localStorage`; httpOnly cookies would be safer.
- Login rate limiting is in memory and resets on restart.
- Account deletion is by email request (hj.min1031@gmail.com), not in the app yet.
- Fonts are loaded from Google Fonts (disclosed in the privacy policy).
- No backups are configured for the Postgres volume.
