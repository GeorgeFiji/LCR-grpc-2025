# Kill any existing Java processes
Write-Host "Stopping any existing Java processes..." -ForegroundColor Yellow
Stop-Process -Name java -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# JVM arguments to force TCP/IP and disable Unix sockets
$javaArgs = "-Djava.net.preferIPv4Stack=true -Dio.grpc.netty.shaded.io.netty.transport.noNative=true"

# Start PeerRegister in a new window
Write-Host "Starting PeerRegister..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "java $javaArgs -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.PeerRegister"

# Wait for PeerRegister to start
Write-Host "Waiting for PeerRegister to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

# Start 8 nodes in separate windows
for ($i = 1; $i -le 8; $i++) {
    Write-Host "Starting Node $i..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "java $javaArgs -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node $i"
    Start-Sleep -Seconds 2
}

Write-Host "`nAll nodes started! Type 'election' in any node window to start an election." -ForegroundColor Green
