#!/usr/bin/env bash
set -e # halt script on error

rm -f /home/travis/.rvm/gems/ruby-2.3.3/gems/faraday-0.12.0/faraday-0.12.0.gem
bundle config disable_checksum_validation true
bundle install
bundle exec jekyll build
#bundle exec htmlproofer ./_site
