package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for working with Digital object identifiers (DOIs) and Short DOIs
 *
 * @see https://en.wikipedia.org/wiki/Digital_object_identifier
 * @see http://shortdoi.org
 */
public class DOI implements Identifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(DOI.class);

    // DOI/Short DOI resolver
    private static final URI RESOLVER = URI.create("https://doi.org");
    // Regex
    // (see http://www.doi.org/doi_handbook/2_Numbering.html)
    private static final String DOI_EXP = ""
            + "(?:urn:)?"                       // optional urn
            + "(?:doi:)?"                       // optional doi
            + "("                               // begin group \1
            + "10"                              // directory indicator
            + "(?:\\.[0-9]+)+"                  // registrant codes
            + "[/:%]" // divider
            + "(?:.+)"                          // suffix alphanumeric string
            + ")";                              // end group \1
    private static final String FIND_DOI_EXP = ""
            + "(?:urn:)?"                       // optional urn
            + "(?:doi:)?"                       // optional doi
            + "("                               // begin group \1
            + "10"                              // directory indicator
            + "(?:\\.[0-9]+)+"                  // registrant codes
            + "[/:]"                            // divider
            + "(?:[^\\s]+)"                     // suffix alphanumeric without space
            + ")";                              // end group \1

    // Regex (Short DOI)
    private static final String SHORT_DOI_EXP = ""
            + "(?:urn:)?"                       // optional urn
            + "(?:doi:)?"                       // optional doi
            + "("                               // begin group \1
            + "10"                              // directory indicator
            + "[/:%]"                            // divider
            + "[a-zA-Z0-9]+"
            + ")";                              // end group \1
    private static final String FIND_SHORT_DOI_EXP = ""
            + "(?:urn:)?"                       // optional urn
            + "(?:doi:)?"                       // optional doi
            + "("                               // begin group \1
            + "10"                              // directory indicator
            + "[/:]"                            // divider
            + "[a-zA-Z0-9]+"
            + "(?:[^\\s]+)"                     // suffix alphanumeric without space
            + ")";                              // end group \1

    private static final String HTTP_EXP = "https?://[^\\s]+?" + DOI_EXP;
    private static final String SHORT_DOI_HTTP_EXP = "https?://[^\\s]+?" + SHORT_DOI_EXP;
    // Pattern
    private static final Pattern EXACT_DOI_PATT = Pattern.compile("^(?:https?://[^\\s]+?)?" + DOI_EXP + "$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOI_PATT = Pattern.compile("(?:https?://[^\\s]+?)?" + FIND_DOI_EXP, Pattern.CASE_INSENSITIVE);
    // Pattern (short DOI)
    private static final Pattern EXACT_SHORT_DOI_PATT = Pattern.compile("^(?:https?://[^\\s]+?)?" + SHORT_DOI_EXP, Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORT_DOI_PATT = Pattern.compile("(?:https?://[^\\s]+?)?" + FIND_SHORT_DOI_EXP, Pattern.CASE_INSENSITIVE);
    // DOI
    private final String doi;
    // Short DOI
    private boolean isShortDoi;

    /**
     * Creates a DOI from various schemes including URL, URN, and plain DOIs/Short DOIs.
     *
     * @param doi the DOI/Short DOI string
     * @throws NullPointerException     if DOI/Short DOI is null
     * @throws IllegalArgumentException if doi does not include a valid DOI/Short DOI
     */
    public DOI(String doi) {
        Objects.requireNonNull(doi);

        // Remove whitespace
        String trimmedDoi = doi.trim();

        // HTTP URL decoding
        if (doi.matches(HTTP_EXP) || doi.matches(SHORT_DOI_HTTP_EXP)) {
            try {
                // decodes path segment
                URI url = new URI(trimmedDoi);
                trimmedDoi = url.getScheme() + "://" + url.getHost() + url.getPath();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(doi + " is not a valid HTTP DOI/Short DOI.");
            }
        }

        // Extract DOI/Short DOI
        Matcher matcher = EXACT_DOI_PATT.matcher(trimmedDoi);
        if (matcher.find()) {
            // match only group \1
            this.doi = matcher.group(1);
        } else {
            // Short DOI
            Matcher shortDoiMatcher = EXACT_SHORT_DOI_PATT.matcher(trimmedDoi);
            if (shortDoiMatcher.find()) {
                this.doi = shortDoiMatcher.group(1);
                isShortDoi = true;
            } else {
                throw new IllegalArgumentException(trimmedDoi + " is not a valid DOI/Short DOI.");
            }
        }
    }

    /**
     * Creates an Optional<DOI> from various schemes including URL, URN, and plain DOIs.
     *
     * Useful for suppressing the <c>IllegalArgumentException</c> of the Constructor and checking for
     * Optional.isPresent() instead.
     *
     * @param doi the DOI/Short DOI string
     * @return an Optional containing the DOI or an empty Optional
     */
    public static Optional<DOI> parse(String doi) {
        try {
            String cleanedDOI = doi.trim();
            cleanedDOI = doi.replaceAll(" ", "");
            return Optional.of(new DOI(cleanedDOI));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    /**
     * Determines whether a DOI/Short DOI is valid or not
     *
     * @param doi the DOI/Short DOI string
     * @return true if DOI is valid, false otherwise
     */
    public static boolean isValid(String doi) {
        return parse(doi).isPresent();
    }

    /**
     * Tries to find a DOI/Short DOI inside the given text.
     *
     * @param text the Text which might contain a DOI/Short DOI
     * @return an Optional containing the DOI or an empty Optional
     */
    public static Optional<DOI> findInText(String text) {
        Optional<DOI> result = Optional.empty();

        Matcher matcher = DOI_PATT.matcher(text);
        if (matcher.find()) {
            // match only group \1
            result = Optional.of(new DOI(matcher.group(1)));
        }

        matcher = SHORT_DOI_PATT.matcher(text);
        if (matcher.find()) {
            result = Optional.of(new DOI(matcher.group(1)));
        }

        return result;
    }

    @Override
    public String toString() {
        return "DOI{" +
                "doi='" + doi + '\'' +
                '}';
    }

    /**
     * Return the plain DOI/Short DOI
     *
     * @return the plain DOI/Short DOI value.
     */
    public String getDOI() {
        return doi;
    }

    /**
     * Determines whether DOI is short DOI or not
     *
     * @return true if DOI is short DOI, false otherwise
     */
    public boolean isShortDoi() {
        return isShortDoi;
    }

    /**
     * Return a URI presentation for the DOI/Short DOI
     *
     * @return an encoded URI representation of the DOI/Short DOI
     */
    @Override
    public Optional<URI> getExternalURI() {
        try {
            URI uri = new URI(RESOLVER.getScheme(), RESOLVER.getHost(), "/" + doi, null);
            return Optional.of(uri);
        } catch (URISyntaxException e) {
            // should never happen
            LOGGER.error(doi + " could not be encoded as URI.", e);
            return Optional.empty();
        }
    }

    /**
     * Return an ASCII URL presentation for the DOI/Short DOI
     *
     * @return an encoded URL representation of the DOI/Short DOI
     */
    public String getURIAsASCIIString() {
        return getExternalURI().map(URI::toASCIIString).orElse("");
    }

    @Override
    public Field getDefaultField() {
        return StandardField.DOI;
    }

    @Override
    public String getNormalized() {
        return doi;
    }
}
