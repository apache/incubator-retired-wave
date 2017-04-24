# Apache Wave

Welcome Apache Wave Maintainer.

This document is your cheat sheet to the resources which are spread around the Apache Wave
repositories and websites. Many of the Apache specific documentation will be on the Wiki linked
below while non-apache related items will be on the website.

### Helpful Links

- [Confluence Wiki](https://cwiki.apache.org/confluence/display/WAVE/Maintainer+Documentation)
- [Maintainer Website](https://incubator.apache.org/wave/maintainers)


### Helper Tool

In the Apache Wave repository is a helper tool which is designed to make our lives easier. The 
tool is ever growing to meet the demands of the project. The maintainers website is the stable 
location for documentation on the tool.

**Required: You must have go 1.7+ installed.**

With go installed you can run the tool with: 

- `go run helper/cli.go`
- `go run helper/cli.go -h`
- `go run helper/cli.go -help`

It will produce files in the `.helper` directory which should not be used for permanent storage
 as `-clean` will remove the entire folder.

**Note: Once new website has been made and published formal docs will be located in the 
maintainers section.**