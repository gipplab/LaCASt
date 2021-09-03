#!/usr/bin/python3

import os
import re
import time
import progressbar
import logging

progressbar.streams.wrap_stderr()
logging.basicConfig()

pattern = re.compile('(\\d+\.\\d+|C\\d+)\\.txt')
numPattern = re.compile('C?(\\d+)(?:\\.(\\d+))?\\.txt')
files = os.listdir("../misc/Mediawiki")

def sortHelper(element):
    temp = numPattern.match(element)
    if temp.group(2):
        return int(temp.group(1))*100+int(temp.group(2))
    else:
        return int(temp.group(1))*100

widgets=[
    progressbar.Variable('file_name', width=9),
    progressbar.Counter(format='%(percentage)3d%%'),
    ' (',
    progressbar.Counter(format='%(value)d of %(max_value)d'),
    ') ',
    progressbar.Bar(),
    progressbar.Timer(),
    ' (', progressbar.ETA(), ') ',
]

files.sort(key = lambda element : sortHelper(element))
with progressbar.ProgressBar(max_value=len(files), redirect_stdout=True, widgets=widgets) as bar:
    for (i, file) in enumerate(files):
        matcher = pattern.match(file)
        if matcher:
            if matcher.group(1).startswith("C"):
                idxStr=matcher.group(1)[1:]
#                 print(int(idxStr))
        bar.update(i, file_name=file)
#         print(file)
        time.sleep(0.1)