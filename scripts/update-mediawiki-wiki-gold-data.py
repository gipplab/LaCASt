#!/usr/bin/python3

# this mediawiki uploader is based on the example from
# https://www.mediawiki.org/wiki/API:Edit

import requests
import sys, getopt
from git import Repo
import os
import re
import progressbar
import time

progressbar.streams.wrap_stderr()

repo = Repo("..")
updatedFilePaths=[file.a_path for file in repo.index.diff(None)]

file_pattern = re.compile('(\\d+)\\.txt')
file_path_base="../misc/Results/Wikipedia/gold/"
files=os.listdir(file_path_base)

try:
    opts, args = getopt.getopt(sys.argv[1:], 'hu:p:', ["help", "user=", "password=", "purge"])
except getopt.GetoptError:
    print("You need to specify username and password to edit the wiki\n\t update-mediawiki-results.py -u <bot-username> -p <bot-password>")
    sys.exit(2)

username=""
password=""
purge_mode=False

for opt, arg in opts:
    if opt in ('-h', "--help"):
        print("You need to specify username and password to edit the wiki\n\t update-mediawiki-results.py -u <bot-username> -p <bot-password>")
        sys.exit()
    elif opt in ("-u", "--user", "--username"):
        username = str(arg)
    elif opt in ("-p", "-pw", "--password"):
        password = str(arg)
    elif opt in ("--purge"):
        purge_mode=True


if username == "" or password == "":
    print("You need to specify username and password to edit the wiki\n\t update-mediawiki-results.py -u <bot-username> -p <bot-password>")
    sys.exit()


session = requests.Session()

# Even though the wiki namespace is "wiki", the api is "w"
URL = "https://sigir21.wmflabs.org/w/api.php"

# Step 1: GET request to fetch login token
get_login_params = {
    "action": "query",
    "meta": "tokens",
    "type": "login",
    "format": "json"
}

response = session.get(url=URL, params=get_login_params)
# print(response.status_code)
# print(response.headers)
response_data = response.json()

login_token = response_data['query']['tokens']['logintoken']

# Step 2: POST request to log in. Use of main account for login is not
# supported. Obtain credentials via Special:BotPasswords
# (https://www.mediawiki.org/wiki/Special:BotPasswords) for lgname & lgpassword
post_login_request_params = {
    "action": "login",
    "lgname": username,
    "lgpassword": password,
    "lgtoken": login_token,
    "format": "json"
}

response = session.post(URL, data=post_login_request_params)
if response.status_code != 200:
    print("The given credentials seems to be invalid")
    print(response.json())
    sys.exit(1)

# Step 3: GET request to fetch CSRF token
get_csrf_token = {
    "action": "query",
    "meta": "tokens",
    "format": "json"
}

response = session.get(url=URL, params=get_csrf_token)
response_data = response.json()

csrf_token = response_data['query']['tokens']['csrftoken']

def uploadFile(i, file_path, page_title):
#     print("Start uploading file " + str(i) + ": " + page_title + " [Path: " + file_path + "]")
#     if file_path[3:] not in updatedFilePaths:
#         print("This page has not been updated locally according to Git, so no need to push changes")
#         return

    file = open(file_path, "r")
    edit_request = {
        "action": "edit",
        "recreate": True,
        "title": page_title,
        "token": csrf_token,
        "format": "json",
        "text": file.read()
    }
    response = session.post(URL, data=edit_request)
    if response.status_code != 200:
        print("[ERROR] Unable to upload " + file_path + ": " + str(response.status_code))
        response_data = response.json()
        print(response_data)

def purge(page_titles):
    purge_request = {
        "action": "purge",
        "titles": '|'.join(page_titles),
        "token": csrf_token,
        "format": "json"
    }
    response = session.post(URL, data=purge_request)
    if response.status_code != 200:
        print("[ERROR] Unable to upload error code: " + str(response.status_code))
        response_data = response.json()
        print(response_data)
    else:
        print("Successfully purged all sites")

widgets=[
    progressbar.Variable('file_name', width=9),
    progressbar.Counter(format='%(percentage)3d%%'),
    ' (',
    progressbar.Counter(format='%(value)d of %(max_value)d'),
    ') ',
    progressbar.Bar(), ' ',
    progressbar.Timer(),
    ' (', progressbar.ETA(), ') ',
]

print("Received CSRF token. Start updating")
with progressbar.ProgressBar(max_value=len(files), redirect_stdout=True, widgets=widgets) as bar:
    for i, file_name in enumerate(files):
        matcher = file_pattern.match(file_name)
        if not matcher:
            print("[ERROR] Cannot read file with name " + file_name)
            continue

        page_title=matcher.group(1)

        uploadFile(i, file_path_base + file_name, "Gold "+page_title)
        bar.update(i, file_name=file_name)
#         time.sleep(0.5)

print("Finished uploading all files. Wiki updated")