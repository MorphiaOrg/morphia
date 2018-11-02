SUBDIRS = reference landing

.PHONY: subdirs $(SUBDIRS) ../target

all: $(SUBDIRS)

$(SUBDIRS):
	@$(MAKE) -s -C $@

../target/gh-pages:
	mkdir -p ../target
	git clone $(shell git config --get remote.origin.url) -b gh-pages ../target/gh-pages

publish: all ../target/gh-pages
	@cd ../target/gh-pages ; git pull
	@$(foreach var, $(SUBDIRS), $(MAKE) -s -C $(var) publish;)
	cd ../target/gh-pages ; git add . && git commit -a -m "pushing docs updates"
	cd ../target/gh-pages && git push

clean:
	@$(foreach var,$(SUBDIRS),$(MAKE) -s -C $(var) clean;)
	@rm -rf ../target/gh-pages
