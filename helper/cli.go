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
A Simple helper program to help maintainers run the project.

Current Feature List:
	- Runs style checkers
	- Runs style formatters ( todo )
	- Runs build tools ( todo )
	- Runs pre installers ( todo )
	- Branch pull requests ( todo )
 	- Publishes website ( todo )

Command line Flags:

	-h | --help 	: shows the help.
	-clean 		: removes all temporary files.

@author: wisebaldone@apache.org ( Evan Hughes )
 */
package main

import (
	"os"
	"fmt"
	"./style"
	"flag"
	"log"
)

var openingText string = `
	                         Apache Wave Maintainer Helper!
	                         ------------------------------

This tool is written for Apache Wave maintainers and may have undesired affects if used by other
developers. This tool will need certain permissions which are not given to it automatically and will
need to prompt the user for credentials.

Use -h or --help to see the list of commands.
----------------------------------------------------------------------------------------------------

`
/*
	Main Helper entry.
 */
func main() {
	// Welcome user.
	fmt.Println(openingText)
	// create temp folder
	os.Mkdir(".helper", 0777)

	clearFlag := flag.Bool("clear", false, "Clears all temporary files ( deletes .helper )")
	styleFlag := flag.Bool("style", false, "Run the style checker on wave and pst projects.")
	flag.Parse()

	if (*clearFlag) {
		clear()
	}
	if (*styleFlag) {
		style.Run()
	}
}

// Deletes the .helper folder
func clear() {
	log.Println("Removing all temporary files.")
	os.RemoveAll(".helper")
}