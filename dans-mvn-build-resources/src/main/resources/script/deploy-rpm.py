#!/usr/bin/env python
#
# Copyright (C) 2018 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import glob
import os
import re
import requests
import sys

from requests.auth import HTTPBasicAuth

snapshot_pattern = re.compile('^.*/[^/]+SNAPSHOT[^/]+\.rpm$')


def is_snapshot(rpm):
    return snapshot_pattern.match(rpm) is not None


def deploy_rpm(nexus_account, nexus_password, repo_url, build_dir):
    rpms = glob.glob("%s/rpm/*/RPMS/*/*.rpm" % build_dir)
    if len(rpms) == 1:
        rpm = rpms[0]
        with open(rpm, 'rb') as f:
            response = requests.put(repo_url + os.path.basename(rpm), data=f, auth=HTTPBasicAuth(nexus_account, nexus_password))
            if response.status_code != 200:
                raise Exception("RPM could not be deployed to repository " + repo_url +
                                ". Status: " + str(response.status_code) + " " + response.reason)
    else:
        raise Exception("Expected 1 RPM found %s, paths: %s" % (len(rpms), ','.join(rpms)))


def print_usage():
    print "Uploads a SNAPSHOT RPM to the Nexus Yum repository"
    print "Usage: ./deploy-rpm.py <nexus_account> <nexus_password> <repo-url> <build_dir>"


if __name__ == '__main__':
    if len(sys.argv) < 4:
        print_usage()
        exit(1)

    nexus_account = sys.argv[1]
    nexus_password = sys.argv[2]
    repo_url = sys.argv[3]
    build_dir = sys.argv[4]

    deploy_rpm(nexus_account, nexus_password, repo_url, build_dir)
    print "Deployed RPM to %s" % repo_url
