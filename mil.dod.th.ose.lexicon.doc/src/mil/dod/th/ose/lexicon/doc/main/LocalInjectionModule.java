//==============================================================================
// This software is part of the Open Standard for Unattended Sensors (OSUS)
// reference implementation (OSUS-R).
//
// To the extent possible under law, the author(s) have dedicated all copyright
// and related and neighboring rights to this software to the public domain
// worldwide. This software is distributed without any warranty.
//
// You should have received a copy of the CC0 Public Domain Dedication along
// with this software. If not, see
// <http://creativecommons.org/publicdomain/zero/1.0/>.
//==============================================================================
package mil.dod.th.ose.lexicon.doc.main;

import com.google.inject.AbstractModule;

import mil.dod.th.ose.lexicon.doc.markup.MarkupGenerator;
import mil.dod.th.ose.lexicon.doc.parser.LexiconParser;

/**
 * Guice injection module for dependency injection.
 * 
 * @author cweisenborn
 */
public class LocalInjectionModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(LexiconParser.class);
        bind(MarkupGenerator.class);
        bind(LexiconDocGeneratorMain.class);
    }
}
