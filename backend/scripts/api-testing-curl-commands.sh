# ============================================================================
# MATRIMONY APP - API TESTING WITH cURL
# ============================================================================

# Set base URL
BASE_URL="http://localhost:5000/api"
API_KEY="Bearer YOUR_ACCESS_TOKEN_HERE"

# ============================================================================
# AUTHENTICATION ENDPOINTS
# ============================================================================

# 1. Request OTP
curl -X POST "$BASE_URL/auth/request-otp" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+919876543210",
    "type": "signup"
  }'

# 2. Verify OTP
curl -X POST "$BASE_URL/auth/verify-otp" \
  -H "Content-Type: application/json" \
  -d '{
    "otp_id": "otp_uuid_here",
    "otp": "123456",
    "phone": "+919876543210"
  }' | jq '.access_token' | xargs -I {} echo "Token: {}"

# 3. Login with Email
curl -X POST "$BASE_URL/auth/login-email" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'

# 4. Refresh Token
curl -X POST "$BASE_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "refresh_token_here"
  }'

# 5. Logout
curl -X POST "$BASE_URL/auth/logout" \
  -H "Authorization: $API_KEY"

# ============================================================================
# PROFILE ENDPOINTS
# ============================================================================

# 6. Get User Profile
curl -X GET "$BASE_URL/profile" \
  -H "Authorization: $API_KEY"

# 7. Update Profile
curl -X PUT "$BASE_URL/profile" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "first_name": "Rahul",
    "last_name": "Kumar",
    "bio": "Software engineer, love travel",
    "hobbies": ["coding", "travel", "gaming"]
  }'

# 8. Upload Photo
curl -X POST "$BASE_URL/profile/photos" \
  -H "Authorization: $API_KEY" \
  -F "file=@/path/to/photo.jpg" \
  -F "is_primary=true"

# 9. Delete Photo
curl -X DELETE "$BASE_URL/profile/photos/{photoId}" \
  -H "Authorization: $API_KEY"

# ============================================================================
# FEED & DISCOVERY ENDPOINTS
# ============================================================================

# 10. Get Feed (with pagination)
curl -X GET "$BASE_URL/feed?page=1&limit=10" \
  -H "Authorization: $API_KEY"

# 11. Search Profiles
curl -X GET "$BASE_URL/search?age_min=25&age_max=35&religions=Hindu,Sikh&cities=Mumbai,Bangalore&limit=20" \
  -H "Authorization: $API_KEY"

# 12. Save Search Preset
curl -X POST "$BASE_URL/search/presets" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Ideal Match",
    "filters": {
      "age_range": [25, 35],
      "religions": ["Hindu"],
      "cities": ["Mumbai"],
      "min_education": "bachelors"
    }
  }'

# 13. Get Saved Presets
curl -X GET "$BASE_URL/search/presets" \
  -H "Authorization: $API_KEY"

# ============================================================================
# MATCHING & INTEREST ENDPOINTS
# ============================================================================

# 14. Send Like (Express Interest)
curl -X POST "$BASE_URL/likes/send" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "receiver_id": "user_uuid_here"
  }'

# 15. Get Matches
curl -X GET "$BASE_URL/matches?tab=mutual&limit=20&page=1" \
  -H "Authorization: $API_KEY"

# Get Received Likes
curl -X GET "$BASE_URL/matches?tab=received&limit=20&page=1" \
  -H "Authorization: $API_KEY"

# Get Sent Likes
curl -X GET "$BASE_URL/matches?tab=sent&limit=20&page=1" \
  -H "Authorization: $API_KEY"

# 16. Accept Like
curl -X POST "$BASE_URL/likes/{likeId}/accept" \
  -H "Authorization: $API_KEY"

# 17. Reject Like
curl -X POST "$BASE_URL/likes/{likeId}/reject" \
  -H "Authorization: $API_KEY"

# ============================================================================
# CHAT & MESSAGING ENDPOINTS
# ============================================================================

# 18. Get Chats List
curl -X GET "$BASE_URL/chats?limit=20&page=1" \
  -H "Authorization: $API_KEY"

# 19. Get Chat Detail
curl -X GET "$BASE_URL/chats/{matchId}?limit=50&page=1" \
  -H "Authorization: $API_KEY"

# 20. Send Message (REST - for testing, WebSocket in production)
curl -X POST "$BASE_URL/messages/send" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "match_id": "match_uuid_here",
    "content": "Hi, how are you doing?"
  }'

# ============================================================================
# VERIFICATION ENDPOINTS
# ============================================================================

# 21. Request Phone Verification
curl -X POST "$BASE_URL/verify/phone-request" \
  -H "Authorization: $API_KEY"

# 22. Verify Phone OTP
curl -X POST "$BASE_URL/verify/phone" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "otp": "123456"
  }'

# 23. Upload ID for Verification
curl -X POST "$BASE_URL/verify/id" \
  -H "Authorization: $API_KEY" \
  -F "front_image=@/path/to/id_front.jpg" \
  -F "back_image=@/path/to/id_back.jpg" \
  -F "id_type=aadhar"

# 24. Get Verification Status
curl -X GET "$BASE_URL/verify/status" \
  -H "Authorization: $API_KEY"

# ============================================================================
# PAYMENT & SUBSCRIPTION ENDPOINTS
# ============================================================================

# 25. Get Plans
curl -X GET "$BASE_URL/plans"

# 26. Create Payment Order
curl -X POST "$BASE_URL/checkout/create-order" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "plan_id": "plan_standard",
    "duration_months": 1
  }'

# 27. Verify Payment
curl -X POST "$BASE_URL/checkout/verify-payment" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "razorpay_order_id": "order_uuid_here",
    "razorpay_payment_id": "pay_uuid_here",
    "razorpay_signature": "signature_here"
  }'

# 28. Get Current Subscription
curl -X GET "$BASE_URL/subscription/current" \
  -H "Authorization: $API_KEY"

# 29. Cancel Subscription
curl -X POST "$BASE_URL/subscription/cancel" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Not interested anymore"
  }'

# ============================================================================
# ADMIN ENDPOINTS
# ============================================================================

# 30. Get Users List (Admin Only)
curl -X GET "$BASE_URL/admin/users?page=1&limit=50&status=active" \
  -H "Authorization: $API_KEY"

# 31. Ban User
curl -X POST "$BASE_URL/admin/users/{userId}/ban" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Harassment and inappropriate behavior",
    "duration_days": 30
  }'

# 32. Get Pending Verifications
curl -X GET "$BASE_URL/admin/verifications/pending?limit=20&page=1" \
  -H "Authorization: $API_KEY"

# 33. Approve Verification
curl -X POST "$BASE_URL/admin/verifications/{verificationId}/approve" \
  -H "Authorization: $API_KEY"

# 34. Reject Verification
curl -X POST "$BASE_URL/admin/verifications/{verificationId}/reject" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Document not clear, edges cut off"
  }'

# 35. Get Reports
curl -X GET "$BASE_URL/admin/reports?status=open&limit=20&page=1" \
  -H "Authorization: $API_KEY"

# 36. Resolve Report
curl -X POST "$BASE_URL/admin/reports/{reportId}/resolve" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "suspend",
    "notes": "User suspended for 7 days due to harassment"
  }'

# 37. Get Payments
curl -X GET "$BASE_URL/admin/payments?status=completed&limit=50&page=1" \
  -H "Authorization: $API_KEY"

# ============================================================================
# USEFUL HELPERS
# ============================================================================

# Extract and save token for future use
# Run this and copy the token that's in response
TOKEN=$(curl -s -X POST "$BASE_URL/auth/verify-otp" \
  -H "Content-Type: application/json" \
  -d '{"otp_id":"otp_id","otp":"123456","phone":"+919876543210"}' | jq -r '.access_token')
echo "Token saved: $TOKEN"

# Test endpoint health
curl -X GET "$BASE_URL/health" 2>&1 | jq .

# Get pretty-printed response
# Add | jq at the end of any curl command to pretty-print JSON

# Rate limit check (headers)
curl -i -X GET "$BASE_URL/feed?page=1" \
  -H "Authorization: $API_KEY" | grep -i "ratelimit"
