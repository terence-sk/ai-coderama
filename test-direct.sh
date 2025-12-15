#!/bin/bash

# Ignore .curlrc file that has issues
alias curl='curl --disable'

echo "Step 1: Login..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}')

echo "Login response:"
echo "$LOGIN_RESPONSE" | jq .

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token')
USER_ID=$(echo "$LOGIN_RESPONSE" | jq -r '.userId')

echo ""
echo "Extracted token: ${TOKEN:0:50}..."
echo "Extracted userId: $USER_ID"
echo ""

echo "Step 2: Create order..."
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"userId\": $USER_ID,
    \"total\": 1299.99,
    \"status\": \"PENDING\",
    \"items\": [
      {
        \"productId\": 1,
        \"quantity\": 1,
        \"price\": 1299.99
      }
    ]
  }")

echo "Order response:"
echo "$ORDER_RESPONSE" | jq .

ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id')

if [ "$ORDER_ID" != "null" ] && [ -n "$ORDER_ID" ]; then
  echo ""
  echo "✓ Success! Order created with ID: $ORDER_ID"
  echo ""
  echo "Waiting 6 seconds for payment processing..."
  sleep 6

  echo ""
  echo "Step 3: Check order status..."
  STATUS_RESPONSE=$(curl -s -X GET http://localhost:8080/api/orders/$ORDER_ID \
    -H "Authorization: Bearer $TOKEN")

  echo "$STATUS_RESPONSE" | jq .

  FINAL_STATUS=$(echo "$STATUS_RESPONSE" | jq -r '.status')
  echo ""
  echo "Final order status: $FINAL_STATUS"
else
  echo ""
  echo "✗ Failed to create order"
fi
