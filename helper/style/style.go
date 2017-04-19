//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

/*
Commands for running the style checkers.

- Java uses stylechecker.

@author: wisebaldone@apache.org ( Evan Hughes )
 */
package style

import (
	"os"
	"log"
	"net/http"
	"io"
	"os/exec"
)

const checkStyle = ".helper/checkstyle.jar"

/*
	Runs stylechecker based off the Google style guide.

	runs on:
		wave/src/main/java
		pst/src/main/java
 */
func Run() {
	_, err := os.Stat(checkStyle)
	if os.IsNotExist(err) {
		download()
	}
	run("wave/src/main/java/", ".helper/wave.style.xml")
	run("pst/src/main/java/", ".helper/pst.style.xml")
}

// Downloads a set version of checkstyle, current version ( 7.6.1 )
func download() {
	out, err := os.Create(checkStyle)
	if err != nil {
		log.Fatalln(err)
	}
	log.Println("Downloading checkstyle.")
	resp, err := http.Get(
		"https://nchc.dl.sourceforge.net/project/checkstyle/checkstyle/" +
			"7.6.1/checkstyle-7.6.1-all.jar")
	if err != nil {
		log.Fatalln("Could not download checkstyle.")
	}
	defer resp.Body.Close()
	_, err = io.Copy(out, resp.Body)
	if err != nil {
		log.Fatalln(err)
	}
}

// Runs the style checker on the input and records the output in xml
func run(directory string, outputFile string) {
	log.Printf("Running checkstyle on %s outputting to %s\n", directory, outputFile)
	cmd := exec.Command(
		"java", "-jar",
		checkStyle,
		"-c", "/google_checks.xml",
		directory,
		"-o",
		outputFile,
		"-f",
		"xml")
	err := cmd.Run()
	if err != nil {
		log.Fatalln(err)
	}
	log.Printf("Finished checking %s\n", directory)
}