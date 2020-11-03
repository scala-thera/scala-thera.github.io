#!/usr/bin/env python

"""
Pandoc filter to import code from some directory.
"""

import os
import sys
from subprocess import Popen, PIPE, call

from pandocfilters import *


def log(txt):
  out = open("./include-code.log","a")
  out.write(txt + '\n')
  out.flush()
  out.close()

def read_snippet(file, snippet_name = None, ignore_other = False, dedent = 3):
  f = open(file, 'r')
  lines = []
  start_from = None

  def append_line(l):
    if not (ignore_other and (
      'start snippet' in l or 'end snippet' in l)):
      if (len(l) > 0): lines.append(l)
      else: lines.append('\n') # Ensure line breaks are present at each line

  for l in f:
    if snippet_name:
      if 'end snippet ' + snippet_name in l: start_from = None

      if start_from: append_line(l[start_from-dedent:])

      idx = l.find('start snippet ' + snippet_name)
      if idx >= 0: start_from = idx
    else:
      append_line(l)

  f.close()
  return ''.join(lines)

def to_dict(keyvals):
  res = {}
  for [k, v] in keyvals: res[k] = v
  return res

def include_code(key, value, fmt, meta):
  if key == 'CodeBlock':
    [[ident, classes, keyvals], code] = value
    kvs = to_dict(keyvals)

    include      = kvs.get("include")
    snippet      = kvs.get("snippet")
    ignore_other = kvs.get("ignore-other-snippets")

    if include:
      src = read_snippet(include, snippet, ignore_other)
      return CodeBlock([ident, classes, keyvals], src)

if __name__ == "__main__":
  toJSONFilter(include_code)