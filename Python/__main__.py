"""
Entrypoint for all common scripting infrastructure.

Invoke like 'python resources <actions>'
Where actions can be any list of actions to take.

"""

from argparse import ArgumentParser
from mcresources import ResourceManager, utils
from typing import Optional

import os
import sys
import json
import difflib

import data
import assets
import recipes
import constants
import world_gen
# import format_lang
# import advancements
# import generate_book
import generate_trees
import generate_textures
# import validate_assets

BOOK_LANGUAGES = ('zh_cn', 'ko_kr', 'zh_tw')
MOD_LANGUAGES = ('zh_cn', 'ru_ru', 'ko_kr', 'pt_br', 'es_es', 'ja_jp')


def main():
    parser = ArgumentParser(description='Entrypoint for all common scripting infrastructure.')
    parser.add_argument('actions', nargs='+', choices=(
        'clean',  # clean all resources (assets / data), including book
        'validate',  # validate no resources are changed when re-running
        'validate_assets',  # manual validation for certain important resources
        'all',  # generate all resources (assets / data)
        'assets',  # only assets.py
        'data',  # only data.py
        'recipes',  # only recipes.py
        'worldgen',  # only world gen data (excluding tags)
        'advancements',  # only advancements.py (which excludes recipe advancements)
        'book',  # generate the book
        'trees',  # generate tree NBT structures from templates
        'format_lang',  # format language files
        'update_lang',  # useful to update localizations after a change to the base which renders some translations incorrect
        'textures',  # generate textures
    ))
    parser.add_argument('--translate', type=str, default='en_uk', help='Runs the book translation using a single provided language')
    parser.add_argument('--translate-all', action='store_true', dest='translate_all', help='Runs the book against all provided translations')
    parser.add_argument('--local', type=str, default=None, help='Points to a local minecraft instance. Used for \'book\', to generate a hot reloadable book, and used for \'clean\', to clean said instance\'s book')
    parser.add_argument('--hotswap', action='store_true', dest='hotswap', help='Causes resource generation to also generate to --hotswap-dir')
    parser.add_argument('--hotswap-dir', type=str, default='./out/production/resources', help='Used for \'--hotswap\'')

    args = parser.parse_args()
    hotswap = args.hotswap_dir if args.hotswap else None

    for action in args.actions:
        if action == 'clean':
            clean(args.local)
        elif action == 'validate':
            validate_resources()
        # elif action == 'validate_assets':
        #     validate_assets.main()
        elif action == 'all':
            resources(hotswap=hotswap, do_assets=True, do_data=True, do_recipes=True, do_worldgen=True, do_advancements=True)
        elif action == 'assets':
            resources(hotswap=hotswap, do_assets=True)
        elif action == 'data':
            resources(hotswap=hotswap, do_data=True)
        elif action == 'recipes':
            resources(hotswap=hotswap, do_recipes=True)
        elif action == 'worldgen':
            resources(hotswap=hotswap, do_worldgen=True)
        elif action == 'advancements':
            resources(hotswap=hotswap, do_advancements=True)
        elif action == 'textures':
            generate_textures.main()
        # elif action == 'book':
        #     if args.translate_all:
        #         for lang in BOOK_LANGUAGES:
        #             generate_book.main(lang, args.local, False)
        #     else:
        #         generate_book.main(args.translate, args.local, False)
        elif action == 'trees':
            generate_trees.main()
        elif action == 'format_lang':
            format_lang.main(False, MOD_LANGUAGES)
        elif action == 'update_lang':
            format_lang.update(MOD_LANGUAGES)


def clean(local: Optional[str]):
    """ Cleans all generated resources files """
    clean_at('E:/Documents/GitHub/Therighthon/ArborFirmaCraft/src/main/resources')
    if local:
        clean_at(local)

def clean_at(location: str):
    for tries in range(1, 1 + 3):
        try:
            utils.clean_generated_resources(location)
            print('Clean %s' % location)
            return
        except OSError:
            print('Failed, retrying (%d / 3)' % tries)
    print('Clean Aborted')


def validate_resources():
    """ Validates all resources are unchanged. """
    rm = ValidatingResourceManager('tfc', 'E:/Documents/GitHub/Therighthon/ArborFirmaCraft/src/main/resources')
    resources_at(rm, True, True, True, True, True)
    error = rm.error_files != 0

    # for lang in BOOK_LANGUAGES:
    #     try:
    #         generate_book.main(lang, None, True, rm)
    #         error |= rm.error_files != 0
    #     except AssertionError as e:
    #         print(e)
    #         error = True
    #
    # for lang in MOD_LANGUAGES:
    #     try:
    #         format_lang.main(True, (lang,))
    #     except AssertionError as e:
    #         print(e)
    #         error = True

    assert not error, 'Validation Errors Were Present'


def resources(hotswap: str = None, do_assets: bool = False, do_data: bool = False, do_recipes: bool = False, do_worldgen: bool = False, do_advancements: bool = False):
    """ Generates resource files, or a subset of them """
    resources_at(ResourceManager('afc', resource_dir='./src/main/resources'), do_assets, do_data, do_recipes, do_worldgen, do_advancements)
    if hotswap:
        resources_at(ResourceManager('afc', resource_dir=hotswap), do_assets, do_data, do_recipes, do_worldgen, do_advancements)


def resources_at(rm: ResourceManager, do_assets: bool, do_data: bool, do_recipes: bool, do_worldgen: bool, do_advancements: bool):
    # do simple lang keys first, because it's ordered intentionally
    # rm.lang(constants.DEFAULT_LANG)

    # generic assets / data
    if do_assets:
        assets.generate(rm)
    if do_data:
        data.generate(rm)
    if do_recipes:
        recipes.generate(rm)
    if do_worldgen:
        world_gen.generate(rm)
    # if do_advancements:
    #     advancements.generate(rm)

    if all((do_assets, do_data, do_worldgen, do_recipes, do_advancements)):
        # Only generate this when generating all, as it's shared
        rm.flush()

    print('New = %d, Modified = %d, Unchanged = %d, Errors = %d' % (rm.new_files, rm.modified_files, rm.unchanged_files, rm.error_files))


class ValidatingResourceManager(ResourceManager):

    def __init__(self, domain: str, resource_dir):
        super(ValidatingResourceManager, self).__init__(domain, resource_dir)
        self.validation_error = False

    def write(self, path_parts, data_to_write):
        data_to_write = utils.del_none({'__comment__': 'This file was automatically created by mcresources', **data_to_write})
        path = os.path.join(*path_parts) + '.json'
        try:
            if not os.path.isfile(path):
                print('Error: resource generation created new file \'%s\'' % path, file=sys.stderr)
                self.error_files += 1
                return
            with open(path, 'r', encoding='utf-8') as file:
                old_data = json.load(file)
            if old_data != data_to_write:
                old_text = json.dumps(old_data, indent=self.indent)
                text = json.dumps(data_to_write, indent=self.indent)
                diff = '\n'.join(difflib.unified_diff(old_text.split('\n'), text.split('\n'), 'old', 'new', n=1))
                print('Error: resource generation modified file \'%s\' Diff:\n%s\n' % (path, diff), file=sys.stderr)
                self.error_files += 1
        except Exception as e:
            self.on_error(path, e)
            self.error_files += 1


if __name__ == '__main__':
    main()
