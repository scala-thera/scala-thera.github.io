#!/usr/bin/env python
# Credits: https://github.com/jgm/pandocfilters

"""
Pandoc filter to process code blocks with class "plantuml" into
plant-generated images.

Needs `plantuml.jar` from http://plantuml.com/.
"""

import os
import sys
from subprocess import call

from pandocfilters import toJSONFilter, Para, Image, Link, get_filename4code, get_caption, get_extension


def plantuml(key, value, format, _):
    if key == 'CodeBlock':
        [[ident, classes, keyvals], code] = value

        if "plantuml" in classes:
            caption, typef, keyvals = get_caption(keyvals)

            filename = get_filename4code("plantuml", code)
            filetype = get_extension(format, "png", html="svg", latex="eps")

            src = filename + '.uml'
            dest = filename + '.' + filetype

            if not os.path.isfile(dest):
                txt = code.encode(sys.getfilesystemencoding())
                if not txt.startswith("@start"):
                    txt = "@startuml\n" + txt + "\n@enduml\n"
                with open(src, "w") as f:
                    f.write(txt)

                call(["plantuml", "-t"+filetype, src])
                sys.stderr.write('Created image ' + dest + '\n')

            absDest = '/'+dest
            img = Image([ident, [], keyvals], caption, [absDest, typef])
            lnk = Link (['', [], []], [img], [absDest, typef])
            return Para([lnk])

if __name__ == "__main__":
    toJSONFilter(plantuml)