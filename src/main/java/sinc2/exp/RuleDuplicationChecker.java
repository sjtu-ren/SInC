package sinc2.exp;

import sinc2.common.ParsedArg;
import sinc2.common.ParsedPred;
import sinc2.rule.Rule;
import sinc2.rule.RuleParseException;

import java.io.*;
import java.util.*;

public class RuleDuplicationChecker {

    /**
     * Split rule pairs in the input file to two files. The input file contains pairs of rules that will be tested for
     * duplication. Each line contains a string of a single rule. Rules in line 1 & 2 are a pair, so do 3 & 4, 5 & 6, ...
     * Rule pairs that are duplicated will be collected in one file, and those are not will be in another.
     */
    public static void main(String[] args) throws IOException, RuleParseException {
        if (1 > args.length) {
            System.err.println("Usage: <Path to file that contains pairs of rules>");
            return;
        }
        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        PrintWriter positive_writer = new PrintWriter("dup_rules_positive.txt");
        PrintWriter negative_writer = new PrintWriter("dup_rules_negative.txt");
        while (true) {
            String rule1_str = reader.readLine();
            String rule2_str = reader.readLine();
            if (null == rule1_str) {
                break;
            }
            List<ParsedPred> rule1 = Rule.parseStructure(rule1_str);
            List<ParsedPred> rule2 = Rule.parseStructure(rule2_str);
            if (matchRules(rule1, rule2)) {
                /* Positive */
                positive_writer.println(rule1_str);
                positive_writer.println(rule2_str);
            } else {
                /* Negative */
                negative_writer.println(rule1_str);
                negative_writer.println(rule2_str);
            }
        }
        reader.close();
        positive_writer.close();
        negative_writer.close();
    }

    /**
     * Check the semantic equivalence of two rules by brute force. Variable IDs in the rules should range from 0 to n-1,
     * where n is the number of LVs in the rule.
     */
    static boolean matchRules(List<ParsedPred> rule1, List<ParsedPred> rule2) {
        /* Collect rule1 LVs */
        int rule1_max_lv_id = -1;
        for (ParsedPred predicate: rule1) {
            for (ParsedArg argument: predicate.args) {
                if (null != argument && null == argument.name) {
                    rule1_max_lv_id = Math.max(rule1_max_lv_id, argument.id);
                }
            }
        }

        /* Collect and normalize rule2 LVs */
        int rule2_max_lv_id = -1;
        for (ParsedPred predicate: rule2) {
            for (ParsedArg argument: predicate.args) {
                if (null != argument && null == argument.name) {
                    rule2_max_lv_id = Math.max(rule2_max_lv_id, argument.id);
                }
            }
        }
        if (rule1_max_lv_id != rule2_max_lv_id) {
            return false;
        }

        /* Permute LVs in rule2 and check equality */
        int[] rule2_lv_2_rul1_lv_mapping = new int[rule1_max_lv_id+1];
        for (int i = 0; i <= rule1_max_lv_id; i++) {
            rule2_lv_2_rul1_lv_mapping[i] = i;
        }
        return permuteRule2VarMapping(0, rule2_lv_2_rul1_lv_mapping, rule2, rule1.get(0), new HashSet<>(rule1.subList(1, rule1.size())));
    }

    static boolean permuteRule2VarMapping(int startIdx, int[] rule2Lv2Rule1LvIds, List<ParsedPred> rule2, ParsedPred head1, Set<ParsedPred> body1) {
        if (startIdx >= rule2Lv2Rule1LvIds.length - 1) {
            /* Check */
            return head1.equals(rule2.get(0)) && body1.equals(new HashSet<>(rule2.subList(1, rule2.size())));
        }
        for (int i = startIdx; i < rule2Lv2Rule1LvIds.length; i++) {
            swap(rule2Lv2Rule1LvIds, rule2, i, startIdx);
            if (permuteRule2VarMapping(startIdx+1, rule2Lv2Rule1LvIds, rule2, head1, body1)) {
                return true;
            }
            swap(rule2Lv2Rule1LvIds, rule2, i, startIdx);
        }
        return false;
    }

    static void swap(int[] rule2Lv2Rule1LvIds, List<ParsedPred> rule2, int idx1, int idx2) {
        if (idx1 == idx2) {
            return;
        }
        final int id1 = rule2Lv2Rule1LvIds[idx1];
        final int id2 = rule2Lv2Rule1LvIds[idx2];
        rule2Lv2Rule1LvIds[idx1] = id2;
        rule2Lv2Rule1LvIds[idx2] = id1;
        for (ParsedPred predicate: rule2) {
            for (int arg_idx = 0; arg_idx < predicate.args.length; arg_idx++) {
                ParsedArg argument = predicate.args[arg_idx];
                if (null != argument && null == argument.name) {
                    if (id1 == argument.id) {
                        predicate.args[arg_idx] = ParsedArg.variable(id2);
                    } else if (id2 == argument.id) {
                        predicate.args[arg_idx] = ParsedArg.variable(id1);
                    }
                }
            }
        }
    }
}
