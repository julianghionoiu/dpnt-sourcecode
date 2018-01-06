import time
import BaseHTTPServer
import os
from urlparse import parse_qs
import json
import sys

HOST_NAME = 'localhost' # !!!REMEMBER TO CHANGE THIS!!!
PORT_NUMBER = 9556 # Maybe set this to 9000.

GIT_REPOS_DIR = ""

class MyHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_POST(self):
        log_info("[POST] You accessed path: %s" % self.path)
        log_debug("[POST] Your request looks like: %s" % self)
        #content = convert_raw_http_request_data_to_string(self)
        #log_info("[POST] Body: %s" % content)
        if (self.path.startswith('/api/v3/repos/')):
            log_info("Creating repository")

    def do_GET(self):
        log_info("[GET] You accessed path: %s" % self.path)
        log_debug("[GET] Your request looks like: %s" % self)
        if (self.path.startswith('/api/v3/repos/')):
            self.get_repo_data()

    def get_repo_data(self):
        print "Getting repo data"
        self.send_response(200)
        self.send_header('Content-type', 'text/json')
        self.end_headers()
        self.wfile.write(json.dumps({
            "id": "1234",
            "clone_url": "file://" + os.path.abspath(GIT_REPOS_DIR)
        }))

def convert_raw_http_request_data_to_string(request):
    contentLength = int(request.headers.getheader('content-length'))
    return request.rfile.read(contentLength)

def log_debug(message):
    log("[DEBUG] " + message)

def log_info(message):
    log("[INFO] " + message)

def log(message):
    print time.asctime(), message

if __name__ == '__main__':
    server_class = BaseHTTPServer.HTTPServer
    httpd = server_class((HOST_NAME, PORT_NUMBER), MyHandler)
    GIT_REPOS_DIR = sys.argv[1]
    log_info("Server Starts - %s:%s" % (HOST_NAME, PORT_NUMBER))
    try:
            httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()
log_info("Server Stops - %s:%s" % (HOST_NAME, PORT_NUMBER))
