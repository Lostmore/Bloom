FROM python:3.12-slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends openssl \
    && rm -rf /var/lib/apt/lists/* \
    && pip install --no-cache-dir PyYAML==6.0.2
WORKDIR /workspace
ENTRYPOINT ["python", "infrastructure/prepare-ci.py", "--output", ".local", "--reuse"]
