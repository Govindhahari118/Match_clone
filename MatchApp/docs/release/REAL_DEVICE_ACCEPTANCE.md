# Matree real-device acceptance matrix

Production promotion requires two independent physical Android devices and two independent member accounts on the exact candidate build.

## Core journey
1. install/upgrade candidate;
2. register or sign in;
3. complete onboarding including profile-created-for;
4. edit profile and upload media;
5. discover/filter profiles;
6. shortlist;
7. send an interest with and without a personal introduction;
8. accept/decline and verify duplicate/block behavior;
9. create a match and exchange chat messages;
10. receive foreground/background/killed-app notifications;
11. exercise quiet hours;
12. block/report;
13. sign out, sign back in, reinstall and recover;
14. complete account deletion and verify post-deletion access is denied.

## Privacy/device behavior
Validate screenshot blocking, recent-app preview privacy, screen recording behavior, user opt-out persistence, Android notification permission denial/grant, reboot, token rotation and multi-device sign-out.

## Network matrix
Repeat critical paths on normal network, constrained/slow network, temporary loss and reconnect. Failed writes must not fabricate success.

## Acceptance record
Record device model, Android version, app version/versionCode, exact Git SHA, account pair, timestamp, result and evidence references. Any unresolved P0/P1 makes the candidate NO-GO.
