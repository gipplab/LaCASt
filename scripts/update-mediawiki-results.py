#!/usr/bin/python3
import requests
import sys, getopt

pageNames = [
  "Algebraic and Analytic Methods",
  "Asymptotic Approximations",
  "Numerical Methods",
  "Elementary Functions",
  "Gamma Function",
  "Exponential, Logarithmic, Sine, and Cosine Integrals",
  "Error Functions, Dawson’s and Fresnel Integrals",
  "Incomplete Gamma and Related Functions",
  "Airy and Related Functions",
  "Bessel Functions", #10
  "Struve and Related Functions",
  "Parabolic Cylinder Functions",
  "Confluent Hypergeometric Functions",
  "Legendre and Related Functions",
  "Hypergeometric Function",
  "Generalized Hypergeometric Functions and Meijer G-Function",
  "q-Hypergeometric and Related Functions",
  "Orthogonal Polynomials", # ====================== 18 ========================
  "Elliptic Integrals",
  "Theta Functions",
  "Multidimensional Theta Functions",
  "Jacobian Elliptic Functions",
  "Weierstrass Elliptic and Modular Functions",
  "Bernoulli and Euler Polynomials",
  "Zeta and Related Functions",
  "Combinatorial Analysis",
  "Functions of Number Theory",
  "Mathieu Functions and Hill’s Equation",
  "Lamé Functions",
  "Spheroidal Wave Functions",
  "Heun Functions",
  "Painlevé Transcendents",
  "Coulomb Functions",
  "3j,6j,9j Symbols",
  "Functions of Matrix Argument",
  "Integrals with Coalescing Saddles",
]

file_path_base="../misc/Mediawiki/"

try:
    opts, args = getopt.getopt(sys.argv[1:], 'hu:p:')
except getopt.GetoptError:
    print("You need to specify username and password to edit the wiki\n\t update-mediawiki-results.py -u <bot-username> -p <bot-password>")
    sys.exit(2)

username=""
password=""

for opt, arg in opts:
    if opt == '-h':
        print("You need to specify username and password to edit the wiki\n\t update-mediawiki-results.py -u <bot-username> -p <bot-password>")
        sys.exit()
    elif opt in ("-u", "--user", "--username"):
        username = str(arg)
    elif opt in ("-p", "-pw", "--password"):
        password = str(arg)

if username == "" or password == "":
    print("You need to specify username and password to edit the wiki\n\t update-mediawiki-results.py -u <bot-username> -p <bot-password>")
    sys.exit()


session = requests.Session()

URL = "https://lct.wmflabs.org/w/api.php"

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
    print("Start uploading file " + str(i) + ": " + page_title + " [Path: " + file_path + "]")
    file = open(file_path, "r")
    edit_request = {
        "action": "edit",
        "title": page_title,
        "token": csrf_token,
        "format": "json",
        "text": file.read()
    }
    response = session.post(URL, data=edit_request)
    if response.status_code == 200:
        print("Successfully uploaded file " + str(i))
    else:
        print("Received error code: " + str(response.status_code))
        response_data = response.json()
        print(response_data)

print("Received CSRF token. Start updating")
for i, name in enumerate(pageNames):
    page_title="Results_of_" + name.replace(" ", "_")
    file_path=file_path_base + str(i+1) + ".txt"
    if i == 3 or i == 9 or i == 12 or i == 13 or i == 14 or i == 17 or i == 18:
        uploadFile(i, file_path, page_title+"_I")
        file_path=file_path_base + str(i+1) + "_2.txt"
        uploadFile(i, file_path, page_title+"_II")
        if i == 9:
            file_path=file_path_base + str(i+1) + "_3.txt"
            uploadFile(i, file_path, page_title+"_III")
    else:
        uploadFile(i, file_path, page_title)

print("Finished uploading all files. Wiki updated")