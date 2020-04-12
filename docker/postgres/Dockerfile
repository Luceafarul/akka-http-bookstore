FROM postgres:12

ENV POSTGRES_PASSWORD bookstore
ENV POSTGRES_USER bookstore

ADD create_db.sql /docker-entrypoint-initdb.d/
