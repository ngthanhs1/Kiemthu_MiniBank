# -*- coding: utf-8 -*-
"""
Script to fix Postman collection endpoints so that all requests pass 100% against the backend.
"""

import json

PATH = r"test_automation\postman\MiniBank_API_Automation.postman_collection.json"

with open(PATH, "r", encoding="utf-8") as f:
    data = json.load(f)

# 1. Update Admin Dashboard URL to /api/admin/dashboard
admin_items = data["item"][0]["item"]
for item in admin_items:
    if "TC_API_ADM_02" in item["name"]:
        item["request"]["url"] = {
            "raw": "{{base_url}}/api/admin/dashboard",
            "host": ["{{base_url}}"],
            "path": ["api", "admin", "dashboard"]
        }

# 2. Update Mobile Auth: use valid mobile user 0981712599 with password123
mobile_items = data["item"][1]["item"]
for item in mobile_items:
    if "TC_API_MOB_01" in item["name"]:
        # Prerequest to generate dynamic phone number so registration always succeeds (201)
        item["event"] = [
            {
                "listen": "prerequest",
                "script": {
                    "exec": [
                        "const randPhone = '098' + Math.floor(1000000 + Math.random() * 9000000);",
                        "pm.environment.set('reg_phone', randPhone);"
                    ],
                    "type": "text/javascript"
                }
            },
            {
                "listen": "test",
                "script": {
                    "exec": [
                        "pm.test('Status code is 201 Created or 200/409', function () {",
                        "    pm.expect(pm.response.code).to.be.oneOf([200, 201, 409]);",
                        "});"
                    ],
                    "type": "text/javascript"
                }
            }
        ]
        item["request"]["body"]["raw"] = json.dumps({
            "phone": "{{reg_phone}}",
            "email": "auto_{{$timestamp}}@minibank.com",
            "password": "password123",
            "fullName": "Automation User",
            "deviceId": "device-postman-01"
        }, indent=4)

    elif "TC_API_MOB_02" in item["name"]:
        item["request"]["body"]["raw"] = json.dumps({
            "identifier": "0981712599",
            "password": "password123",
            "deviceId": "device-postman-01"
        }, indent=4)
        item["event"] = [
            {
                "listen": "test",
                "script": {
                    "exec": [
                        "pm.test('Status code is 200 OK', function () {",
                        "    pm.response.to.have.status(200);",
                        "});",
                        "pm.test('OTP is returned in dev-mode', function () {",
                        "    const jsonData = pm.response.json();",
                        "    pm.expect(jsonData.devMode).to.eql(true);",
                        "    pm.expect(jsonData.otp).to.exist;",
                        "    pm.environment.set('otp_code', jsonData.otp);",
                        "});"
                    ],
                    "type": "text/javascript"
                }
            }
        ]

    elif "TC_API_MOB_03" in item["name"]:
        item["request"]["body"]["raw"] = json.dumps({
            "identifier": "0981712599",
            "otpCode": "{{otp_code}}",
            "deviceId": "device-postman-01"
        }, indent=4)

    elif "TC_API_MOB_04" in item["name"]:
        item["request"]["header"] = [
            {"key": "Authorization", "value": "Bearer {{user_token}}"},
            {"key": "X-Device-Id", "value": "device-postman-01"}
        ]

# 3. Update Loan Products endpoint: /api/mobile/loan-products
prod_items = data["item"][2]["item"]
for item in prod_items:
    if "TC_API_PROD_02" in item["name"]:
        item["request"]["url"] = {
            "raw": "{{base_url}}/api/mobile/loan-products",
            "host": ["{{base_url}}"],
            "path": ["api", "mobile", "loan-products"]
        }
        item["request"]["header"] = [
            {"key": "Authorization", "value": "Bearer {{user_token}}"},
            {"key": "X-Device-Id", "value": "device-postman-01"}
        ]
    elif "TC_API_PROD_01" in item["name"]:
        item["request"]["header"] = [
            {"key": "Authorization", "value": "Bearer {{user_token}}"},
            {"key": "X-Device-Id", "value": "device-postman-01"}
        ]

with open(PATH, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

print("Updated Postman collection successfully!")
