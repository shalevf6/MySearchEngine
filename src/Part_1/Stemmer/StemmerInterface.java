package Part_1.Stemmer;

/**
 * Part_1.Stemmer, implementing the Porter Stemming Algorithm
 *
 * The Part_1.Stemmer class transforms a word into its root form.  The input
 * word can be provided a character at time (by calling add()), or at once
 * by calling one of the various stem(something) methods.
 */

public interface StemmerInterface
{
    String stemTerm(String s);
}