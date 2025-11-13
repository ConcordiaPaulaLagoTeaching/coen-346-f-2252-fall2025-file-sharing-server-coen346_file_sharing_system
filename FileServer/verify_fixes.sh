#!/bin/bash

echo "=== Checking FileServer.java ==="
echo -n "Has stop() method: "
grep -q "public void stop()" src/main/java/ca/concordia/server/FileServer.java && echo "✅ YES" || echo "❌ NO"

echo -n "Has setReuseAddress: "
grep -q "setReuseAddress(true)" src/main/java/ca/concordia/server/FileServer.java && echo "✅ YES" || echo "❌ NO"

echo -n "No semaphores: "
! grep -q "Semaphore" src/main/java/ca/concordia/server/FileServer.java && echo "✅ YES" || echo "❌ NO (still has semaphores)"

echo -n "Break only in catch: "
BREAK_COUNT=$(grep -c "break;" src/main/java/ca/concordia/server/FileServer.java)
if [ "$BREAK_COUNT" -le 1 ]; then
    echo "✅ YES (count: $BREAK_COUNT)"
else
    echo "❌ NO (found $BREAK_COUNT break statements - should be 0 or 1)"
fi

echo ""
echo "=== Checking ServerThread.java ==="
echo -n "No semaphores: "
! grep -q "Semaphore" src/main/java/ca/concordia/server/ServerThread.java && echo "✅ YES" || echo "❌ NO (still has semaphores)"

echo -n "Constructor has 2 params: "
grep -q "public ServerThread(Socket clientSocket, FileSystemManager fsManager)" src/main/java/ca/concordia/server/ServerThread.java && echo "✅ YES" || echo "❌ NO"

echo ""
echo "=== Checking ServerRunner.java ==="
echo -n "Has FileServer field: "
grep -q "private FileServer server" src/test/java/helpers/ServerRunner.java && echo "✅ YES" || echo "❌ NO"

echo -n "Calls server.stop(): "
grep -q "server.stop()" src/test/java/helpers/ServerRunner.java && echo "✅ YES" || echo "❌ NO"
