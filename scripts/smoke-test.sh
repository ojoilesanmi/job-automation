#!/bin/bash
set -e

echo "Running smoke tests..."

BASE_URL="${BASE_URL:-http://localhost:8080}"

# Test 1: Health check
echo "1. Testing health endpoint..."
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health")
if [ "$HEALTH" != "200" ]; then
  echo "FAIL: Health check returned $HEALTH"
  exit 1
fi
echo "PASS: Health check"

# Test 2: Auth endpoint accessible
echo "2. Testing auth endpoint accessibility..."
AUTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke-test-'$(date +%s)'@test.com","password":"Test1234!","fullName":"Smoke Test"}')
if [ "$AUTH_STATUS" != "201" ] && [ "$AUTH_STATUS" != "400" ]; then
  echo "FAIL: Auth endpoint returned $AUTH_STATUS"
  exit 1
fi
echo "PASS: Auth endpoint accessible"

# Test 3: Jobs endpoint requires auth
echo "3. Testing jobs endpoint requires authentication..."
JOBS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/jobs")
if [ "$JOBS_STATUS" != "401" ] && [ "$JOBS_STATUS" != "403" ]; then
  echo "FAIL: Jobs endpoint returned $JOBS_STATUS without auth"
  exit 1
fi
echo "PASS: Jobs endpoint requires auth"

# Test 4: Prometheus metrics endpoint
echo "4. Testing Prometheus metrics endpoint..."
METRICS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/prometheus")
if [ "$METRICS_STATUS" != "200" ]; then
  echo "FAIL: Metrics endpoint returned $METRICS_STATUS"
  exit 1
fi
echo "PASS: Prometheus metrics endpoint"

# Test 5: AI service health
echo "5. Testing AI service health..."
AI_URL="${AI_SERVICE_URL:-http://localhost:8000}"
AI_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$AI_URL/health" 2>/dev/null || echo "000")
if [ "$AI_HEALTH" != "200" ]; then
  echo "WARN: AI service health returned $AI_HEALTH (non-blocking)"
else
  echo "PASS: AI service health"
fi

echo ""
echo "All smoke tests passed!"
