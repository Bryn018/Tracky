import http.server
import socketserver
import os
import socket

DEFAULT_PORT = 8080
APK_PATH = r"C:\tracky\app\build\outputs\apk\debug\app-debug.apk"

class Handler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path in ("/", "/index.html"):
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write((
                "<html><body>"
                "<h2>Tracky Debug APK</h2>"
                f"<p><a href='/app-debug.apk'>Download APK</a></p>"
                "</body></html>"
            ).encode("utf-8"))
            return
        if self.path == "/app-debug.apk":
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", "attachment; filename=app-debug.apk")
            fs = os.stat(APK_PATH)
            self.send_header("Content-Length", str(fs.st_size))
            self.end_headers()
            with open(APK_PATH, "rb") as f:
                self.wfile.write(f.read())
            return
        return super().do_GET()


def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    finally:
        s.close()
    return ip

if __name__ == "__main__":
    os.chdir(os.path.dirname(APK_PATH))
    port = DEFAULT_PORT
    while port < DEFAULT_PORT + 20:
        try:
            with socketserver.TCPServer(("", port), Handler) as httpd:
                print(f"Serving APK at http://{get_local_ip()}:{port}/app-debug.apk")
                print("Press Ctrl+C to stop")
                httpd.serve_forever()
        except OSError as e:
            if "10048" in str(e):
                port += 1
                continue
            raise
    raise SystemExit("No free port in range 8000-8019")
