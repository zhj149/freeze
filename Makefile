# **********************************************************************
#
# Copyright (c) 2003-2018 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

top_srcdir := .

include $(top_srcdir)/config/Make.rules

define make-global-rule
$1::
	+@for subdir in $2; \
	do \
	    echo "making all in $$$$subdir"; \
	    ( cd $$$$subdir && $(MAKE) $1 ) || exit 1; \
	done
endef

$(eval $(call make-global-rule,srcs,$(languages)))
$(eval $(call make-global-rule,tests,$(languages)))
$(eval $(call make-global-rule,all,$(languages)))
$(eval $(call make-global-rule,clean,$(languages)))
$(eval $(call make-global-rule,distclean,$(languages)))
$(eval $(call make-global-rule,install,$(languages)))

#
# Install documentation and slice files
#
install:: install-doc

$(eval $(call install-data-files,$(wildcard $(top_srcdir)/*LICENSE),$(top_srcdir),$(install_docdir),\
        install-doc,"Installing documentation files"))
