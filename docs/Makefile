SUBDIRS = reference

.PHONY: subdirs $(SUBDIRS) ../target

all: $(SUBDIRS)

$(SUBDIRS):
	@$(MAKE) -s -C $@

../target/gh-pages:
	mkdir -p ../target
	git clone $(shell git config --get remote.origin.url) -b gh-pages ../target/gh-pages

publish: all ../target/gh-pages
	cd ../target/gh-pages ; git pull
	@$(MAKE) -s -C reference publish
	cd ../target/gh-pages ; git commit -a -m "pushing docs updates" && git push

clean:
	@$(MAKE) -s -C reference clean
