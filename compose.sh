#!/usr/bin/sh

docker login registry.gitlab.com -u pavelgordon -p $DOCKER_TOKEN
docker pull registry.gitlab.com/pavelgordon/hvvclient
docker stop hvvclient || true

docker run --name hvvclient --rm -d \
--env "VIRTUAL_HOST=pgordon.dev" \
--env "LETSENCRYPT_HOST=pgordon.dev" \
--env "LETSENCRYPT_EMAIL=pgordon.dev" \
registry.gitlab.com/pavelgordon/hvvclient
