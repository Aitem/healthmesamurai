.EXPORT_ALL_VARIABLES:

repl:
	clj -A:shadow:dev:test watch app
build:
	clj -A:shadow release app
