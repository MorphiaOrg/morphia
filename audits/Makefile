# The operator audits, runnable locally. `make help` lists the targets.
#
#   DOCS      the mongodb/docs checkout (default: target/mongodb-docs; `mvn clean` removes it, and
#             the next target that needs it checks it out again)
#   REVISION  the docs commit to check out (default: the pin in config/manual-baseline-sources.json;
#             HEAD is the docs' head)
#   OUT       where extraction and discovery write their reports (default: target/extraction and
#             target/discovery)

SHELL := bash
.SHELLFLAGS := -euo pipefail -c
.DEFAULT_GOAL := help

# Every path is anchored at this file, so `make` works from here, from the root with `-C audits`
# or `-f audits/Makefile`, and from anywhere else.
HERE := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
ROOT := $(abspath $(HERE)/..)
BIN := $(HERE)/bin
DOCS ?= $(HERE)/target/mongodb-docs
REVISION ?=
OUT ?=
MVN := $(ROOT)/mvnw -B -ntp -q
OPERATOR_TESTS := dev/morphia/test/aggregation/expressions/*,dev/morphia/test/aggregation/stages/*,dev/morphia/test/query/filters/*,dev/morphia/test/query/updates/*,ExampleOwnersTest

# The stages share the docs checkout and Maven's target directories, so they never run at once.
.NOTPARALLEL:

# Checks out the docs at a revision (empty: the pin), or moves the existing checkout to it. Each
# target that reads the docs runs it itself, since Make would run a shared prerequisite only once.
checkout = if [ -d "$(DOCS)/.git" ]; then \
		$(BIN)/checkout-docs.sh --update "$(DOCS)" $(1); \
	else \
		$(BIN)/checkout-docs.sh "$(DOCS)" $(1); \
	fi

.PHONY: help all docs test parser-test extract report core-test discover discover-test clean-docs

help: ## List the targets
	@grep -hE '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  %-14s %s\n", $$1, $$2}'

all: test parser-test extract core-test ## Everything: the module's tests, extraction at the pin with its report, and core's operator tests

docs: ## Check out the docs at REVISION, or move an existing checkout to it
	@$(call checkout,$(REVISION))

test: ## The module's offline tests
	@cd $(ROOT) && $(MVN) -Daudits -pl :morphia-audits verify

parser-test: ## The parser's regressions and the coordinator's integration with it (needs uv)
	@cd $(ROOT) && uv sync --frozen --project audits/parser -q \
		&& audits/parser/.venv/bin/python -I -B audits/parser/tests/test_adapter.py \
		&& python3 -B -m unittest discover -s audits/probes/shell-results -p 'test_*.py' \
		&& $(MVN) -Daudits -pl :morphia-audits test -Dtest=DocumentParsingTest \
			-Daudits.parser.python="$$PWD/audits/parser/.venv/bin/python" \
			-Daudits.parser.adapter="$$PWD/audits/parser/adapter.py"

extract: ## Rebuild the inventory and scenarios in place from the docs at REVISION, then the report; review with git diff
	@$(call checkout,$(REVISION))
	$(BIN)/extract.sh "$(DOCS)" "$(or $(OUT),$(HERE)/target/extraction)"
	@git -C $(ROOT) status --short -- audits/catalog core/src/test/resources
	@$(MAKE) --no-print-directory -f $(HERE)/Makefile report

report: ## Write the missing-scenario report: target/missing-scenarios.md and .json
	@cd $(ROOT) && $(ROOT)/mvnw -B -ntp -Daudits -pl :morphia-audits test -Dtest=ScenarioCoverageTest \
		-Dsurefire.failIfNoSpecifiedTests=false -Dmaven.test.failure.ignore=true \
		| sed -n -e 's/^\[INFO\] \[stdout\] //p' -e '/BUILD FAILURE\|ERROR\] Failed/p'

# The scenarios run against plain Document collections, so the mapper doesn't change what they
# check; build.yml covers mapper parity. One run, under the default mapper.
core-test: ## Core's operator tests against the scenarios in this tree (needs Docker)
	@cd $(ROOT) && $(MVN) install -pl :morphia-core -am -DskipTests -Ddeploy.skip=true -Dinvoker.skip=true \
		&& $(MVN) test -pl :morphia-core -Ddeploy.skip=true -Dinvoker.skip=true \
			-Dsurefire.failIfNoSpecifiedTests=false -Dtest='$(OPERATOR_TESTS)'

discover: ## The weekly discovery against the docs' head (or REVISION), in a throwaway worktree of the committed HEAD; nothing here changes
	@$(call checkout,$(or $(REVISION),HEAD))
	$(BIN)/discover.sh "$(DOCS)" "$(or $(OUT),$(HERE)/target/discovery)"

discover-test: ## discover, then core's operator tests against its result (needs Docker)
	@$(call checkout,$(or $(REVISION),HEAD))
	$(BIN)/discover.sh "$(DOCS)" "$(or $(OUT),$(HERE)/target/discovery)" --test

clean-docs: ## Remove the docs checkout
	rm -rf "$(DOCS)"
