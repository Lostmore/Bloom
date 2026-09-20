GOLANG
```yaml
name: <placeholder_service_name> CI

on:
  push:
    branches: [ "develop" ]
    paths:
      - 'server/<placeholder_service_folder>/**'
      - '.github/workflows/<placeholder_service_name>-ci.yml'
  pull_request:
    branches: [ "develop" ]
    paths:
      - 'server/<placeholder_service_folder>/**'

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    # services:
    #   redis:
    #     image: redis:7
    #     ports:
    #       - 6379:6379
    #   postgres:
    #     image: postgres:17
    #     env:
    #       POSTGRES_DB: <placeholder_db_name>
    #       POSTGRES_USER: <placeholder_db_user>
    #       POSTGRES_PASSWORD: <placeholder_db_password>
    #     ports:
    #       - 5432:5432

    steps:
    - name: Checkout repository
      uses: actions/checkout@v4

    - name: Set up Go
      uses: actions/setup-go@v5
      with:
        go-version: '<placeholder_go_version>'
        cache: true

    - name: Download dependencies
      working-directory: ./server/<placeholder_service_folder>
      run: go mod download

    - name: Run Tests
      working-directory: ./server/<placeholder_service_folder>
      run: go test -v ./...

    - name: Build
      working-directory: ./server/<placeholder_service_folder>
      run: go build -v ./cmd/api
```

JAVA

```yaml
name: <placeholder_service_name> CI

on:
  push:
    branches: [ "develop" ]
    paths:
      - 'server/<placeholder_service_folder>/**'
      - '.github/workflows/<placeholder_service_name>-ci.yml'
  pull_request:
    branches: [ "develop" ]
    paths:
      - 'server/<placeholder_service_folder>/**'

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    # services:
    #   postgres:
    #     image: postgres:17
    #     env:
    #       POSTGRES_DB: <placeholder_db_name>
    #       POSTGRES_USER: <placeholder_db_user>
    #       POSTGRES_PASSWORD: <placeholder_db_password>
    #     ports:
    #       - 5432:5432

    steps:
    - name: Checkout repository
      uses: actions/checkout@v4

    - name: Set up JDK
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x server/gradlew

    - name: Build and Test
      working-directory: ./server
      run: ./gradlew :<placeholder_service_folder>:build
```
