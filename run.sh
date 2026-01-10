#!/bin/bash
export DB_HOST="localhost";
export DB_PORT="5432";
export DB_NAME="irr";
export DB_USER="irr_admin";
export DB_PASSWORD="root";

export POSTGRES_USER="irr_admin";
export POSTGRES_PASSWORD="root";
export POSTGRES_DB="irr";

export JWT_SECRET="tC6YwQrL2R19eTKq/P7m/k5dtESO1BN2srsnwRg3puE=";

mvn spring-boot:run

