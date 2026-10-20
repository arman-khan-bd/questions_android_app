package com.questions.govt.util;

import android.text.TextUtils;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance LyX, KaTeX & LaTeX Math Formula Parser for Android Native TextViews.
 * Seamlessly parses mathematical formulas, fractions, radicals, powers, Greek symbols,
 * integrals, and rich text without requiring heavy WebViews or JavaScript execution.
 */
public final class LyxMathParser {

    private LyxMathParser() {
        // Prevent instantiation
    }

    // Common fraction pattern: \frac{a}{b}
    private static final Pattern PATTERN_FRAC = Pattern.compile("\\\\frac\\{([^{}]+)\\}\\{([^{}]+)\\}");
    // Nested/Complex fraction fallback: \frac{...}{...}
    private static final Pattern PATTERN_FRAC_RECURSIVE = Pattern.compile("\\\\frac\\{([^\\}]+)\\}\\{([^\\}]+)\\}");
    // Nth root: \sqrt[n]{x}
    private static final Pattern PATTERN_NTH_ROOT = Pattern.compile("\\\\sqrt\\[([^\\]]+)\\]\\{([^\\}]+)\\}");
    // Square root: \sqrt{x}
    private static final Pattern PATTERN_SQRT = Pattern.compile("\\\\sqrt\\{([^\\}]+)\\}");
    // Single-char square root without braces: \sqrt x
    private static final Pattern PATTERN_SQRT_SIMPLE = Pattern.compile("\\\\sqrt\\s+([a-zA-Z0-9])");
    // Degree symbol: ^{\circ} or ^\circ
    private static final Pattern PATTERN_DEGREE = Pattern.compile("\\^\\{?\\\\circ\\}?");
    // Text wrapper: \text{...} or \mathrm{...} or \mathbf{...}
    private static final Pattern PATTERN_TEXT = Pattern.compile("\\\\(text|mathrm|mathbf|mathit|operatorname)\\{([^\\}]+)\\}");
    // Bracketed superscript: ^{...}
    private static final Pattern PATTERN_SUPERSCRIPT_BRACES = Pattern.compile("\\^\\{([^\\}]+)\\}");
    // Single-char superscript: ^2 or ^x
    private static final Pattern PATTERN_SUPERSCRIPT_SINGLE = Pattern.compile("\\^([0-9a-zA-Z+\\-=()])");
    // Bracketed subscript: _{...}
    private static final Pattern PATTERN_SUBSCRIPT_BRACES = Pattern.compile("_\\{([^\\}]+)\\}");
    // Single-char subscript: _2 or _x
    private static final Pattern PATTERN_SUBSCRIPT_SINGLE = Pattern.compile("_([0-9a-zA-Z+\\-=()])");
    // Inline / Block math delimiters: $$...$$ or $...$
    private static final Pattern PATTERN_DOLLAR_DELIMITERS = Pattern.compile("\\${1,2}");
    // Math mode \( and \) or \[ and \]
    private static final Pattern PATTERN_PAREN_MATH = Pattern.compile("\\\\[\\(\\)\\[\\]]");

    /**
     * Parse raw string with LyX/LaTeX math notation into formatted CharSequence for Android TextView.
     */
    public static CharSequence parse(String rawText) {
        if (TextUtils.isEmpty(rawText)) {
            return "";
        }

        String text = rawText.trim();

        // 1. Initial HTML cleanup for paragraph tags while preserving intentional line breaks
        text = text.replaceAll("(?i)<p[^>]*>", "")
                   .replaceAll("(?i)</p>", "\n")
                   .replaceAll("(?i)<br\\s*/?>", "\n")
                   .replaceAll("(?i)<div[^>]*>", "")
                   .replaceAll("(?i)</div>", "\n")
                   .replaceAll("(?i)&nbsp;", " ")
                   .replaceAll("(?i)&amp;", "&")
                   .replaceAll("(?i)&lt;", "<")
                   .replaceAll("(?i)&gt;", ">")
                   .replaceAll("(?i)&quot;", "\"")
                   .replaceAll("(?i)&#39;", "'");

        // 2. Parse text wrappers: \text{...}
        Matcher textMatcher = PATTERN_TEXT.matcher(text);
        StringBuffer textSb = new StringBuffer();
        while (textMatcher.find()) {
            textMatcher.appendReplacement(textSb, Matcher.quoteReplacement(textMatcher.group(2)));
        }
        textMatcher.appendTail(textSb);
        text = textSb.toString();

        // 3. Degree symbols: ^\circ -> °
        text = PATTERN_DEGREE.matcher(text).replaceAll("°");

        // 4. Fractions: \frac{a}{b}
        text = parseFractions(text);

        // 5. Radicals: \sqrt[n]{x} and \sqrt{x}
        text = parseRoots(text);

        // 6. Common Greek Letters & Constants
        text = replaceGreekLetters(text);

        // 7. Operators, Relations, Arrows & Special Symbols
        text = replaceMathSymbols(text);

        // 8. Superscripts (x^2 -> x², x^{10} -> x¹⁰)
        text = parseSuperscripts(text);

        // 9. Subscripts (x_1 -> x₁, x_{ab} -> xₐᵦ)
        text = parseSubscripts(text);

        // 10. Functions & Spacing commands
        text = text.replaceAll("\\\\sin\\b", "sin")
                   .replaceAll("\\\\cos\\b", "cos")
                   .replaceAll("\\\\tan\\b", "tan")
                   .replaceAll("\\\\cot\\b", "cot")
                   .replaceAll("\\\\sec\\b", "sec")
                   .replaceAll("\\\\csc\\b", "csc")
                   .replaceAll("\\\\log\\b", "log")
                   .replaceAll("\\\\ln\\b", "ln")
                   .replaceAll("\\\\lim\\b", "lim")
                   .replaceAll("\\\\max\\b", "max")
                   .replaceAll("\\\\min\\b", "min")
                   .replaceAll("\\\\[,;:! ]", " ")
                   .replaceAll("\\\\quad\\b", "   ")
                   .replaceAll("\\\\qquad\\b", "      ")
                   .replaceAll("\\\\left[\\(\\[\\{]", "(")
                   .replaceAll("\\\\right[\\)\\]\\}]", ")")
                   .replaceAll("\\\\\\{", "{")
                   .replaceAll("\\\\\\}", "}");

        // 11. Remove remaining math delimiters ($, $$, \(, \), \[, \])
        text = PATTERN_DOLLAR_DELIMITERS.matcher(text).replaceAll("");
        text = PATTERN_PAREN_MATH.matcher(text).replaceAll("");

        // 12. Remove stray backslashes and collapse extra spaces
        text = text.replaceAll("\\\\", "")
                   .replaceAll("[ \\t]+", " ")
                   .replaceAll("\n\\s*\n", "\n\n")
                   .trim();

        // 13. If text contains HTML styling like <b>, <i>, <u>, parse with HtmlCompat
        if (text.contains("<") && text.contains(">")) {
            try {
                return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT);
            } catch (Exception ignored) {
                // Fallback to plain string
            }
        }

        return text;
    }

    /**
     * Safely applies parsed LyX/Math text to a TextView.
     */
    public static void setMathText(TextView textView, String rawText) {
        if (textView == null) return;
        textView.setText(parse(rawText));
    }

    private static String parseFractions(String input) {
        String current = input;
        boolean changed = true;
        int iterations = 0;

        while (changed && iterations < 5) {
            changed = false;
            iterations++;
            Matcher m = PATTERN_FRAC.matcher(current);
            StringBuffer sb = new StringBuffer();

            while (m.find()) {
                changed = true;
                String num = m.group(1).trim();
                String den = m.group(2).trim();
                String replacement = getUnicodeFraction(num, den);
                if (replacement == null) {
                    if (num.length() == 1 && den.length() == 1) {
                        replacement = num + "/" + den;
                    } else {
                        replacement = "(" + num + " / " + den + ")";
                    }
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            current = sb.toString();
        }

        return current;
    }

    private static String getUnicodeFraction(String num, String den) {
        if ("1".equals(num)) {
            switch (den) {
                case "2": return "½";
                case "3": return "⅓";
                case "4": return "¼";
                case "5": return "⅕";
                case "6": return "⅙";
                case "7": return "⅐";
                case "8": return "⅛";
                case "9": return "⅑";
                case "10": return "⅒";
            }
        } else if ("2".equals(num)) {
            if ("3".equals(den)) return "⅔";
            if ("5".equals(den)) return "⅖";
        } else if ("3".equals(num)) {
            if ("4".equals(den)) return "¾";
            if ("5".equals(den)) return "⅗";
            if ("8".equals(den)) return "⅜";
        } else if ("4".equals(num)) {
            if ("5".equals(den)) return "⅘";
        } else if ("5".equals(num)) {
            if ("6".equals(den)) return "⅚";
            if ("8".equals(den)) return "⅝";
        } else if ("7".equals(num)) {
            if ("8".equals(den)) return "⅞";
        }
        return null;
    }

    private static String parseRoots(String input) {
        // Nth root
        Matcher nth = PATTERN_NTH_ROOT.matcher(input);
        StringBuffer sbNth = new StringBuffer();
        while (nth.find()) {
            String degree = toSuperscript(nth.group(1));
            String body = nth.group(2);
            nth.appendReplacement(sbNth, Matcher.quoteReplacement(degree + "√(" + body + ")"));
        }
        nth.appendTail(sbNth);
        String text = sbNth.toString();

        // Square root with braces
        Matcher sqrt = PATTERN_SQRT.matcher(text);
        StringBuffer sbSqrt = new StringBuffer();
        while (sqrt.find()) {
            String body = sqrt.group(1);
            String replacement = (body.length() <= 2 && !body.contains(" ")) ? ("√" + body) : ("√(" + body + ")");
            sqrt.appendReplacement(sbSqrt, Matcher.quoteReplacement(replacement));
        }
        sqrt.appendTail(sbSqrt);
        text = sbSqrt.toString();

        // Square root simple without braces
        text = PATTERN_SQRT_SIMPLE.matcher(text).replaceAll("√$1");

        return text;
    }

    private static String parseSuperscripts(String input) {
        // Handle braces: ^{2} or ^{x+1}
        Matcher m = PATTERN_SUPERSCRIPT_BRACES.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String content = m.group(1);
            String superContent = toSuperscript(content);
            m.appendReplacement(sb, Matcher.quoteReplacement(superContent));
        }
        m.appendTail(sb);
        String text = sb.toString();

        // Handle single-char: ^2, ^x
        Matcher mSingle = PATTERN_SUPERSCRIPT_SINGLE.matcher(text);
        StringBuffer sbSingle = new StringBuffer();
        while (mSingle.find()) {
            String ch = mSingle.group(1);
            String superCh = toSuperscript(ch);
            mSingle.appendReplacement(sbSingle, Matcher.quoteReplacement(superCh));
        }
        mSingle.appendTail(sbSingle);

        return sbSingle.toString();
    }

    private static String parseSubscripts(String input) {
        // Handle braces: _{1} or _{ab}
        Matcher m = PATTERN_SUBSCRIPT_BRACES.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String content = m.group(1);
            String subContent = toSubscript(content);
            m.appendReplacement(sb, Matcher.quoteReplacement(subContent));
        }
        m.appendTail(sb);
        String text = sb.toString();

        // Handle single-char: _1, _x
        Matcher mSingle = PATTERN_SUBSCRIPT_SINGLE.matcher(text);
        StringBuffer sbSingle = new StringBuffer();
        while (mSingle.find()) {
            String ch = mSingle.group(1);
            String subCh = toSubscript(ch);
            mSingle.appendReplacement(sbSingle, Matcher.quoteReplacement(subCh));
        }
        mSingle.appendTail(sbSingle);

        return sbSingle.toString();
    }

    private static String toSuperscript(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '0': sb.append('⁰'); break;
                case '1': sb.append('¹'); break;
                case '2': sb.append('²'); break;
                case '3': sb.append('³'); break;
                case '4': sb.append('⁴'); break;
                case '5': sb.append('⁵'); break;
                case '6': sb.append('⁶'); break;
                case '7': sb.append('⁷'); break;
                case '8': sb.append('⁸'); break;
                case '9': sb.append('⁹'); break;
                case '+': sb.append('⁺'); break;
                case '-': case '−': sb.append('⁻'); break;
                case '=': sb.append('⁼'); break;
                case '(': sb.append('⁽'); break;
                case ')': sb.append('⁾'); break;
                case 'n': sb.append('ⁿ'); break;
                case 'i': sb.append('ⁱ'); break;
                case 'x': sb.append('ˣ'); break;
                case 'y': sb.append('ʸ'); break;
                case 'a': sb.append('ᵃ'); break;
                case 'b': sb.append('ᵇ'); break;
                case 'c': sb.append('ᶜ'); break;
                case 'd': sb.append('ᵈ'); break;
                case 'e': sb.append('ᵉ'); break;
                case 'm': sb.append('ᵐ'); break;
                case 'p': sb.append('ᵖ'); break;
                case 'r': sb.append('ʳ'); break;
                case 't': sb.append('ᵗ'); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    private static String toSubscript(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '0': sb.append('₀'); break;
                case '1': sb.append('₁'); break;
                case '2': sb.append('₂'); break;
                case '3': sb.append('₃'); break;
                case '4': sb.append('₄'); break;
                case '5': sb.append('₅'); break;
                case '6': sb.append('₆'); break;
                case '7': sb.append('₇'); break;
                case '8': sb.append('₈'); break;
                case '9': sb.append('₉'); break;
                case '+': sb.append('₊'); break;
                case '-': case '−': sb.append('₋'); break;
                case '=': sb.append('₌'); break;
                case '(': sb.append('₍'); break;
                case ')': sb.append('₎'); break;
                case 'a': sb.append('ₐ'); break;
                case 'e': sb.append('ₑ'); break;
                case 'o': sb.append('ₒ'); break;
                case 'x': sb.append('ₓ'); break;
                case 'h': sb.append('ₕ'); break;
                case 'k': sb.append('ₖ'); break;
                case 'l': sb.append('ₗ'); break;
                case 'm': sb.append('ₘ'); break;
                case 'n': sb.append('ₙ'); break;
                case 'p': sb.append('ₚ'); break;
                case 's': sb.append('ₛ'); break;
                case 't': sb.append('ₜ'); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    private static String replaceGreekLetters(String text) {
        return text.replaceAll("\\\\alpha\\b", "α")
                   .replaceAll("\\\\beta\\b", "β")
                   .replaceAll("\\\\gamma\\b", "γ")
                   .replaceAll("\\\\Gamma\\b", "Γ")
                   .replaceAll("\\\\delta\\b", "δ")
                   .replaceAll("\\\\Delta\\b", "Δ")
                   .replaceAll("\\\\epsilon\\b|\\\\varepsilon\\b", "ε")
                   .replaceAll("\\\\zeta\\b", "ζ")
                   .replaceAll("\\\\eta\\b", "η")
                   .replaceAll("\\\\theta\\b", "θ")
                   .replaceAll("\\\\Theta\\b", "Θ")
                   .replaceAll("\\\\iota\\b", "ι")
                   .replaceAll("\\\\kappa\\b", "κ")
                   .replaceAll("\\\\lambda\\b", "λ")
                   .replaceAll("\\\\Lambda\\b", "Λ")
                   .replaceAll("\\\\mu\\b", "μ")
                   .replaceAll("\\\\nu\\b", "ν")
                   .replaceAll("\\\\xi\\b", "ξ")
                   .replaceAll("\\\\Xi\\b", "Ξ")
                   .replaceAll("\\\\pi\\b", "π")
                   .replaceAll("\\\\Pi\\b", "Π")
                   .replaceAll("\\\\rho\\b", "ρ")
                   .replaceAll("\\\\sigma\\b", "σ")
                   .replaceAll("\\\\Sigma\\b", "Σ")
                   .replaceAll("\\\\tau\\b", "τ")
                   .replaceAll("\\\\upsilon\\b", "υ")
                   .replaceAll("\\\\phi\\b|\\\\varphi\\b", "φ")
                   .replaceAll("\\\\Phi\\b", "Φ")
                   .replaceAll("\\\\chi\\b", "χ")
                   .replaceAll("\\\\psi\\b", "ψ")
                   .replaceAll("\\\\Psi\\b", "Ψ")
                   .replaceAll("\\\\omega\\b", "ω")
                   .replaceAll("\\\\Omega\\b", "Ω");
    }

    private static String replaceMathSymbols(String text) {
        return text.replaceAll("\\\\pm\\b", "±")
                   .replaceAll("\\\\mp\\b", "∓")
                   .replaceAll("\\\\times\\b", "×")
                   .replaceAll("\\\\div\\b", "÷")
                   .replaceAll("\\\\cdot\\b", "·")
                   .replaceAll("\\\\ast\\b", "*")
                   .replaceAll("\\\\leq?\\b", "≤")
                   .replaceAll("\\\\geq?\\b", "≥")
                   .replaceAll("\\\\neq?\\b", "≠")
                   .replaceAll("\\\\approx\\b", "≈")
                   .replaceAll("\\\\equiv\\b", "≡")
                   .replaceAll("\\\\sim\\b", "∼")
                   .replaceAll("\\\\propto\\b", "∝")
                   .replaceAll("\\\\infty\\b", "∞")
                   .replaceAll("\\\\partial\\b", "∂")
                   .replaceAll("\\\\nabla\\b", "∇")
                   .replaceAll("\\\\angle\\b", "∠")
                   .replaceAll("\\\\forall\\b", "∀")
                   .replaceAll("\\\\exists\\b", "∃")
                   .replaceAll("\\\\notin\\b", "∉")
                   .replaceAll("\\\\in\\b", "∈")
                   .replaceAll("\\\\subset\\b", "⊂")
                   .replaceAll("\\\\subseteq\\b", "⊆")
                   .replaceAll("\\\\cap\\b", "∩")
                   .replaceAll("\\\\cup\\b", "∪")
                   .replaceAll("\\\\int\\b", "∫")
                   .replaceAll("\\\\iint\\b", "∬")
                   .replaceAll("\\\\iiint\\b", "∭")
                   .replaceAll("\\\\sum\\b", "∑")
                   .replaceAll("\\\\prod\\b", "∏")
                   .replaceAll("\\\\implies\\b", "⇒")
                   .replaceAll("\\\\iff\\b", "⇔")
                   .replaceAll("\\\\to\\b|\\\\rightarrow\\b", "→")
                   .replaceAll("\\\\leftarrow\\b", "←")
                   .replaceAll("\\\\leftrightarrow\\b", "↔")
                   .replaceAll("\\\\Rightarrow\\b", "⇒")
                   .replaceAll("\\\\Leftarrow\\b", "⇐")
                   .replaceAll("\\\\%\\b|\\\\%", "%");
    }
}
