package sinc.exp;

import sinc.SincRecovery;
import sinc.common.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestRecover {

    public static void main(String[] args) throws Exception {
        final String result_file_path = args[0];
        final List<Rule> hypothesis = new ArrayList<>();
        final Set<Predicate> reduced_facts = new HashSet<>();
        final Set<Predicate> counter_examples = new HashSet<>();
        final Set<String> supplementary_constants = new HashSet<>();
        load(result_file_path, hypothesis, reduced_facts, counter_examples, supplementary_constants);

        final long time_start = System.currentTimeMillis();
        SincRecovery recovery = new SincRecovery(hypothesis, reduced_facts, counter_examples, supplementary_constants);
        final Set<Predicate> original_kb = recovery.recover();
        final long time_done = System.currentTimeMillis();

        System.out.printf("Recover %d facts in %d ms.", original_kb.size(), time_done - time_start);
    }

    static void load(
            String filePath, List<Rule> hypothesis, Set<Predicate> reducedFacts, Set<Predicate> counterExmaples,
            Set<String> supplementaryConstants
    ) throws IOException  {
        final int STATUS_HYPOTHESIS = 0;
        final int STATUS_REDUCED_FACTS = 1;
        final int STATUS_COUNTER_EXAMPLES = 2;
        final int STATUS_SUPPLEMENTARY_CONSTANTS = 3;
        final int STATUS_DONE = 4;

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        int status = STATUS_HYPOTHESIS;
        while (null != (line = reader.readLine())) {
            line = line.strip();
            switch (status) {
                case STATUS_HYPOTHESIS:
                    if (0 == line.length()) {
                        status = STATUS_REDUCED_FACTS;
                    } else {
                        Rule r = parseRule(line);
                        if (!r.toDumpString().contains(line)) {
                            throw new Error(String.format("Rule Parse Failed: %s (%s)", line, r.toDumpString()));
                        }
                        hypothesis.add(r);
                    }
                    break;
                case STATUS_REDUCED_FACTS:
                    if (0 == line.length()) {
                        status = STATUS_COUNTER_EXAMPLES;
                    } else {
                        reducedFacts.add(parseFact(line));
                    }
                    break;
                case STATUS_COUNTER_EXAMPLES:
                    if (0 == line.length()) {
                        status = STATUS_SUPPLEMENTARY_CONSTANTS;
                    } else {
                        counterExmaples.add(parseFact(line));
                    }
                    break;
                case STATUS_SUPPLEMENTARY_CONSTANTS:
                    if (0 == line.length()) {
                        status = STATUS_DONE;
                    } else {
                        supplementaryConstants.add(line);
                    }
                    break;
                case STATUS_DONE:
                    return;
                default:
                    throw new Error("Unknown Status:" + status);
            }
        }
    }

    static Rule parseRule(String s) {
        final List<String> predicate_strs = new ArrayList<>();
        String[] head_and_body = s.split(":-");
        predicate_strs.add(head_and_body[0]);
        int start_idx = 0;
        int brackets = 0;
        char[] body_chars = head_and_body[1].toCharArray();
        for (int i = 0; i < body_chars.length; i++) {
            char c = body_chars[i];
            switch (c) {
                case ',':
                    if (0 == brackets) {
                        predicate_strs.add(head_and_body[1].substring(start_idx, i));
                        start_idx = i + 1;
                    }
                    break;
                case '(':
                    brackets++;
                    break;
                case ')':
                    brackets--;
            }
        }
        predicate_strs.add(head_and_body[1].substring(start_idx));

        final List<Predicate> rule_structure = new ArrayList<>();
        for (String predicate_str: predicate_strs) {
            rule_structure.add(parsePredicate(predicate_str));
        }
        return new BareRule(rule_structure, new HashSet<>());
    }

    static Predicate parsePredicate(String s) {
        final String[] components = s.split("[(,)]");
        final Predicate predicate = new Predicate(components[0], components.length - 1);
        for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
            final String arg_str = components[arg_idx+1];
            final char first_char = arg_str.charAt(0);
            switch (first_char) {
                case '?':
                    predicate.args[arg_idx] = null;
                    break;
                case 'X':
                    predicate.args[arg_idx] = new Variable(Integer.parseInt(arg_str.substring(1)));
                    break;
                default:
                    predicate.args[arg_idx] = new Constant(Rule.CONSTANT_ARG_ID, arg_str);
            }
        }
        return predicate;
    }

    static Predicate parseFact(String s) {
        final String[] components = s.split("\t");
        final Predicate predicate = new Predicate(components[0], components.length - 1);
        for (int arg_idx = 0; arg_idx < predicate.arity(); arg_idx++) {
            final String arg_str = components[arg_idx+1];
            final char first_char = arg_str.charAt(0);
            switch (first_char) {
                case '?':
                    predicate.args[arg_idx] = null;
                    break;
                case 'X':
                    predicate.args[arg_idx] = new Variable(Integer.parseInt(arg_str.substring(1)));
                    break;
                default:
                    predicate.args[arg_idx] = new Constant(Rule.CONSTANT_ARG_ID, arg_str);
            }
        }
        return predicate;
    }
}
