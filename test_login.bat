@echo off
curl -s -X POST http://127.0.0.1:8080/api/auth/login -H "Content-Type: application/json" -d @test_login.json > test_token.txt 2>&1
type test_token.txt
