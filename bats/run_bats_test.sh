#!/bin/bash
set -e

DIRNAME="$(dirname $0)"
. "$DIRNAME"/utils.sh

export POSTGRES_DOCKER_VERSION="9.6.12"

# Call getopt to validate the provided input.
options=$(getopt -o "" --long postgres_docker_version: -- "$@")
[ $? -eq 0 ] || {
    echo "Incorrect options provided"
    exit 1
}
eval set -- "$options"
while true; do
    case "$1" in
    --postgres_docker_version)
        shift; # The arg is next in position args
        export POSTGRES_DOCKER_VERSION="$1"
        ;;
    --)
        shift
        break
        ;;
    esac
    shift
done

echo "Running test for postgres docker image with version $POSTGRES_DOCKER_VERSION"

trap shutdownDockerContainer EXIT SIGINT

export DATABASE_PORT=15432

sudo docker run --rm --name test-postgres -e POSTGRES_PASSWORD=postgres_posmulten -p 127.0.0.1:$DATABASE_PORT:5432/tcp -d postgres:$POSTGRES_DOCKER_VERSION

export DOCKER_DB_IP="127.0.0.1"
export PGPASSWORD=postgres_posmulten

waitUntilDockerContainerIsReady

SCRIPT_DIR=`resolveScriptDirectory`
psql -qtAX -U postgres -p $DATABASE_PORT --host="$DOCKER_DB_IP" -f "$SCRIPT_DIR/../db_scripts/prepare_postgresql-core_db.sql"


#Run test
set +e
bats -rt "$SCRIPT_DIR/prepare_postgresql-core_db"
DATABASE_TESTS_RESULT="$?"

echo "Running schema structure tests for 'public' schema"
export DATABASE_TESTS_SCHEMA_NAME="public"
bats -rt "$SCRIPT_DIR/schema_structure"
DATABASE_TESTS_PUBLIC_SCHEMA_RESULT="$?"

echo "Running schema structure tests for 'non_public_schema' schema"
export DATABASE_TESTS_SCHEMA_NAME="non_public_schema"
bats -rt "$SCRIPT_DIR/schema_structure"
DATABASE_TESTS_NON_PUBLIC_SCHEMA_RESULT="$?"

[[ $DATABASE_TESTS_RESULT -eq 0 ]] && [[ $DATABASE_TESTS_PUBLIC_SCHEMA_RESULT -eq 0 ]] && [[ $DATABASE_TESTS_NON_PUBLIC_SCHEMA_RESULT -eq 0 ]]

#
# TIPS!
# psql:
# - To quit command line console (no-interactive mode) pass '\q' then press ENTER
# - If docker container is still working, you can login to database by executing below commands:
#   export PGPASSWORD=postgres
#   psql -d postgres -U postgres -p 15432 --host=127.0.0.1
#
# In case problem with error "/usr/local/lib/libldap_r-2.4.so.2: no version information available (required by /usr/lib/x86_64-linux-gnu/libpq.so.5)"
# try https://www.dangtrinh.com/2017/04/how-to-fix-usrlocalliblibldapr-24so2-no.html and execute:
# sudo ln -fs /usr/lib/liblber-2.4.so.2 /usr/local/lib/
# sudo ln -fs /usr/lib/libldap_r-2.4.so.2 /usr/local/lib/
#
