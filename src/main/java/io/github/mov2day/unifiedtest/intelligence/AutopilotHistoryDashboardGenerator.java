package io.github.mov2day.unifiedtest.intelligence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Renders a dependency-free local reliability dashboard from privacy-safe Autopilot history. */
public final class AutopilotHistoryDashboardGenerator {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ROOT)
        .withZone(ZoneId.systemDefault());
    private AutopilotHistoryDashboardGenerator() {}

    public static void generate(Path buildDirectory, RunHistoryStore.HistorySnapshot snapshot, boolean currentHtmlReportEnabled) {
        Path output = buildDirectory.resolve("unifiedtest/intelligence/dashboard.html");
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, render(snapshot, currentHtmlReportEnabled), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // The dashboard is advisory output and must never fail a test task.
        }
    }

    static String render(RunHistoryStore.HistorySnapshot snapshot, boolean currentHtmlReportEnabled) {
        List<RunSummary> runs = snapshot.runs().stream().map(RunSummary::from).toList();
        Map<String, TestTimeline> timelines = timelines(snapshot);
        List<TestTimeline> regressions = timelines.values().stream().filter(TestTimeline::isRegression)
            .sorted(Comparator.comparing(TestTimeline::lastTimestamp).reversed()).toList();
        List<TestTimeline> flaky = timelines.values().stream().filter(TestTimeline::isFlaky)
            .sorted(Comparator.comparingInt(TestTimeline::failures).reversed()).toList();
        List<TestTimeline> known = timelines.values().stream().filter(TestTimeline::isKnownFailure)
            .sorted(Comparator.comparingInt(TestTimeline::matchingFailures).reversed()).toList();
        List<FingerprintCluster> clusters = clusters(snapshot);
        Set<String> tasks = snapshot.runs().stream().map(RunHistoryStore.HistoryRun::displayTask)
            .collect(Collectors.toCollection(TreeSet::new));

        StringBuilder html = new StringBuilder(28000);
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            .append("<title>UnifiedTest Autopilot History</title><style>").append(styles())
            .append("</style></head><body><main class=\"shell\">");
        renderHeader(html, runs, currentHtmlReportEnabled);
        if (runs.isEmpty()) {
            html.append("<section class=\"empty-state\"><p class=\"eyebrow\">Awaiting observations</p><h2>Run a test task to start the timeline.</h2>")
                .append("<p>Local status history only. No raw failures or attachments are retained.</p></section>");
        } else {
            renderViewTabs(html);
            html.append("<section id=\"overview-view\" class=\"dashboard-view\" role=\"tabpanel\" aria-labelledby=\"overview-tab\">");
            renderSummary(html, runs, timelines.size(), regressions, flaky, known);
            renderTrend(html, runs);
            html.append("</section><section id=\"triage-view\" class=\"dashboard-view\" role=\"tabpanel\" aria-labelledby=\"triage-tab\" hidden>");
            renderFilters(html, tasks);
            html.append("<section class=\"layout\"><div class=\"primary\">");
            renderTriage(html, "Recent regressions", "A failure immediately following a passing observation.", regressions, "regression");
            renderTriage(html, "Flaky candidates", "At least two passes and two failures in the latest ten observations.", flaky, "flaky");
            renderTriage(html, "Known failures", "The current failure fingerprint has occurred in two or more earlier runs.", known, "known");
            html.append("</div><aside class=\"secondary\">");
            renderClusters(html, clusters);
            html.append("</aside></section></section>");
        }
        html.append("</main><script>").append(script()).append("</script></body></html>");
        return html.toString();
    }

    private static void renderHeader(StringBuilder html, List<RunSummary> runs, boolean currentHtmlReportEnabled) {
        RunSummary latest = runs.isEmpty() ? null : runs.get(runs.size() - 1);
        html.append("<header class=\"hero\"><div class=\"hero-copy\"><p class=\"eyebrow\">LOCAL TEST INTELLIGENCE</p><h1>Autopilot</h1></div><div class=\"hero-actions\">");
        if (latest != null) {
            html.append("<div class=\"hero-status ").append(latest.healthKind()).append("\"><span>Latest observation</span><strong>")
                .append(esc(latest.healthLabel())).append("</strong><small>").append(latest.passed).append(" passed · ")
                .append(latest.skipped).append(" skipped · ").append(latest.failed).append(" failed</small></div>");
        }
        html.append("<nav aria-label=\"Current run outputs\">");
        html.append("<a class=\"link-button\" href=\"autopilot.json\">JSON</a>");
        if (currentHtmlReportEnabled) html.append("<a class=\"link-button quiet\" href=\"../reports/index.html\">Report</a>");
        html.append("</nav></div></header>");
        if (!runs.isEmpty()) html.append("<p class=\"run-note\">").append(runs.size()).append(" local runs</p>");
    }

    private static void renderViewTabs(StringBuilder html) {
        html.append("<div class=\"view-tabs\" role=\"tablist\" aria-label=\"Dashboard view\">")
            .append("<button id=\"overview-tab\" type=\"button\" role=\"tab\" aria-selected=\"true\" aria-controls=\"overview-view\">Overview</button>")
            .append("<button id=\"triage-tab\" type=\"button\" role=\"tab\" aria-selected=\"false\" aria-controls=\"triage-view\" tabindex=\"-1\">Triage</button></div>");
    }

    private static void renderSummary(StringBuilder html, List<RunSummary> runs, int trackedTests, List<TestTimeline> regressions,
                                      List<TestTimeline> flaky, List<TestTimeline> known) {
        RunSummary latest = runs.get(runs.size() - 1);
        html.append("<section class=\"history-brief\" aria-label=\"Latest reliability summary\"><article class=\"window-card\"><p>History</p><strong>")
            .append(runs.size()).append("</strong><span>runs · ").append(trackedTests).append(" tests</span><small>")
            .append(esc(latest.taskName)).append("</small></article><article class=\"stability-card\"><span>Latest pass rate</span><strong>")
            .append(latest.passRate()).append("<em>%</em></strong><div class=\"bar\" aria-label=\"").append(latest.passRate())
            .append(" percent pass rate\"><i class=\"pass\" style=\"width:").append(latest.pct(latest.passed)).append("%\"></i><i class=\"skip\" style=\"width:")
            .append(latest.pct(latest.skipped)).append("%\"></i><i class=\"fail\" style=\"width:").append(latest.pct(latest.failed)).append("%\"></i></div></article>")
            .append("<article class=\"attention-card\"><p>Needs attention</p><div><span>Regressions</span><b>").append(regressions.size())
            .append("</b></div><div><span>Flaky candidates</span><b>").append(flaky.size()).append("</b></div><div><span>Known failures</span><b>")
            .append(known.size()).append("</b></div></article></section>");
    }

    private static String stat(String label, Object value, String detail, String kind) {
        return "<article class=\"stat " + kind + "\"><span>" + esc(label) + "</span><strong>" + esc(String.valueOf(value))
            + "</strong><small>" + esc(detail) + "</small></article>";
    }

    private static void renderFilters(StringBuilder html, Set<String> tasks) {
        html.append("<section class=\"filters\" aria-label=\"Reliability filters\"><label>Search <input id=\"search\" name=\"search\" type=\"search\" autocomplete=\"off\" spellcheck=\"false\" placeholder=\"Test or class…\"></label>")
            .append("<label>Task <select id=\"taskFilter\" name=\"task\"><option value=\"all\">All tasks</option>");
        for (String task : tasks) html.append("<option value=\"").append(attr(task)).append("\">").append(esc(task)).append("</option>");
        html.append("</select></label><label>Status <select id=\"statusFilter\" name=\"status\"><option value=\"all\">All statuses</option><option value=\"FAIL\">Failed</option><option value=\"PASS\">Passed</option><option value=\"SKIP\">Skipped</option></select></label></section>");
    }

    private static void renderTriage(StringBuilder html, String title, String description, List<TestTimeline> tests, String kind) {
        html.append("<section class=\"panel\"><div class=\"panel-heading\"><h2>")
            .append(esc(title)).append("</h2><span>").append(tests.size()).append("</span></div><p class=\"panel-description\">")
            .append(esc(description)).append("</p>");
        if (tests.isEmpty()) {
            html.append("<p class=\"muted\">None</p>");
        } else {
            html.append("<div class=\"triage-list\">");
            for (TestTimeline test : tests.stream().limit(8).toList()) renderTestRow(html, test, kind);
            html.append("</div>");
        }
        html.append("</section>");
    }

    private static void renderTestRow(StringBuilder html, TestTimeline test, String kind) {
        html.append("<article class=\"test-row\" data-search=\"").append(attr(test.searchText())).append("\" data-task=\"")
            .append(attr(test.taskName)).append("\" data-status=\"").append(attr(test.lastStatus())).append("\"><div class=\"identity\"><span class=\"signal ")
            .append(attr(kind)).append("\">").append(esc(kind)).append("</span><div><h3>").append(esc(test.displayName())).append("</h3><p>")
            .append(esc(test.displayClass())).append("</p></div></div><div class=\"observation\"><span>")
            .append(esc(test.sequence())).append("</span><small title=\"failures / observed runs\">").append(test.failures()).append(" / ").append(test.observations.size()).append("</small></div></article>");
    }

    private static void renderTrend(StringBuilder html, List<RunSummary> runs) {
        List<RunSummary> recent = runs.stream().skip(Math.max(0, runs.size() - 12)).toList();
        html.append("<section class=\"trend-panel\" aria-labelledby=\"health-trend-title\"><div class=\"trend-heading\"><h2 id=\"health-trend-title\">Suite health</h2><span>Last ")
            .append(recent.size()).append(" runs</span></div><ol class=\"trend-chart\">");
        for (int index = 0; index < recent.size(); index++) {
            RunSummary run = recent.get(index);
            html.append("<li><strong>").append(run.passRate()).append("%</strong><div class=\"trend-stack\" role=\"img\" aria-label=\"")
                .append(esc(run.displayTime())).append(", ").append(esc(run.taskName)).append(": ").append(run.passRate()).append(" percent passed, ").append(run.skipped)
                .append(" skipped, ").append(run.failed).append(" failed\"><i class=\"pass\" style=\"height:")
                .append(run.pct(run.passed)).append("%\"></i><i class=\"skip\" style=\"height:").append(run.pct(run.skipped))
                .append("%\"></i><i class=\"fail\" style=\"height:").append(run.pct(run.failed)).append("%\"></i></div><span>")
                .append(index + 1).append("</span></li>");
        }
        html.append("</ol><div class=\"trend-legend\"><span class=\"pass\">Passed</span><span class=\"skip\">Skipped</span><span class=\"fail\">Failed</span></div></section>");
    }

    private static void renderClusters(StringBuilder html, List<FingerprintCluster> clusters) {
        html.append("<section class=\"panel clusters\"><div class=\"panel-heading\"><h2>Recurring failures</h2><span>").append(clusters.size())
            .append("</span></div><p class=\"panel-description\">Matching normalized fingerprints across retained history.</p>");
        if (clusters.isEmpty()) {
            html.append("<p class=\"muted\">None</p>");
        } else {
            html.append("<ol class=\"cluster-list\">");
            for (FingerprintCluster cluster : clusters.stream().limit(6).toList()) {
                html.append("<li><code>").append(esc(cluster.shortFingerprint())).append("</code><span>").append(cluster.failures).append(" failures across ")
                    .append(cluster.tests.size()).append(" tests</span></li>");
            }
            html.append("</ol>");
        }
        html.append("</section>");
    }

    private static Map<String, TestTimeline> timelines(RunHistoryStore.HistorySnapshot snapshot) {
        Map<String, TestTimeline> timelines = new LinkedHashMap<>();
        for (RunHistoryStore.HistoryRun run : snapshot.runs()) {
            for (RunHistoryStore.HistoryTest test : run.tests()) {
                String key = run.displayTask() + "|" + test.testId();
                timelines.computeIfAbsent(key, ignored -> new TestTimeline()).add(run, test);
            }
        }
        return timelines;
    }

    private static List<FingerprintCluster> clusters(RunHistoryStore.HistorySnapshot snapshot) {
        Map<String, FingerprintCluster> values = new LinkedHashMap<>();
        for (RunHistoryStore.HistoryRun run : snapshot.runs()) {
            for (RunHistoryStore.HistoryTest test : run.tests()) {
                if (isFailure(test.status()) && test.fingerprint() != null && !test.fingerprint().isBlank()) {
                    values.computeIfAbsent(test.fingerprint(), FingerprintCluster::new).add(test);
                }
            }
        }
        return values.values().stream().filter(cluster -> cluster.failures >= 2)
            .sorted(Comparator.comparingInt((FingerprintCluster cluster) -> cluster.failures).reversed()).toList();
    }

    private static boolean isPass(String status) { return "PASS".equalsIgnoreCase(status) || "PASSED".equalsIgnoreCase(status); }
    private static boolean isFailure(String status) { return "FAIL".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status); }
    private static String esc(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    private static String attr(String value) { return esc(value); }

    private static String styles() {
        return """
            :root{--ink:#14211f;--muted:#61716b;--paper:#f4f7f5;--panel:#fff;--line:#d5e0db;--green:#146b55;--red:#b8323a;--gold:#9c6618;--violet:#62529e;--wash:#eaf2ee}*{box-sizing:border-box}body{margin:0;background:radial-gradient(circle at 85% 0,#dcece5 0,transparent 28rem),linear-gradient(135deg,#eef5f1,#f9faf8 52%,#e7efeb);color:var(--ink);font:15px/1.5 ui-monospace,SFMono-Regular,Menlo,monospace}.shell{max-width:1280px;margin:auto;padding:34px 24px 64px}.hero{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:28px;align-items:end;padding:0 0 25px;border-bottom:2px solid var(--ink)}.hero h1,h2,h3{font-family:"Palatino Linotype",Iowan Old Style,Georgia,serif;margin:0}.hero h1{font-size:clamp(2.3rem,5vw,4.15rem);line-height:.98;letter-spacing:-.055em}.eyebrow{font-size:.7rem;font-weight:900;letter-spacing:.15em;color:var(--green);margin:0 0 8px;text-transform:uppercase}.subtitle,.muted,.panel-heading>p{color:var(--muted);max-width:640px}.hero-actions{display:grid;justify-items:end;gap:10px}.hero nav{display:flex;gap:8px;flex-wrap:wrap;justify-content:end}.hero-status{display:grid;gap:2px;min-width:240px;padding:10px 13px;border-left:4px solid var(--green);background:rgba(255,255,255,.64)}.hero-status.warn{border-left-color:var(--gold)}.hero-status.fail{border-left-color:var(--red)}.hero-status span,.hero-status small{color:var(--muted);font-size:.72rem}.hero-status strong{text-transform:uppercase;letter-spacing:.06em}.link-button{border:1px solid var(--ink);padding:9px 12px;color:#fff;background:var(--ink);text-decoration:none;font-weight:800;transition:transform .16s ease,background .16s ease}.link-button:hover{transform:translateY(-1px);background:var(--green)}.link-button.quiet{color:var(--ink);background:transparent}.run-note{color:var(--muted);margin:16px 0}.summary{display:grid;grid-template-columns:repeat(4,1fr);gap:0;margin:24px 0;border:1px solid var(--line);background:rgba(255,255,255,.83);box-shadow:0 12px 34px rgba(19,47,38,.06)}.stat{position:relative;padding:17px 16px;min-height:138px;border-left:1px solid var(--line)}.stat:first-child{border-left:0}.stat:before{content:"";position:absolute;top:0;left:0;right:0;height:4px;background:var(--green)}.stat.fail:before{background:var(--red)}.stat.warn:before{background:var(--gold)}.stat.known:before{background:var(--violet)}.stat span,.stat small{display:block;color:var(--muted)}.stat span{font-size:.72rem;font-weight:900;letter-spacing:.09em;text-transform:uppercase}.stat strong{display:block;font:clamp(2rem,3.2vw,2.8rem)/1 "Palatino Linotype",Georgia,serif;margin:17px 0 8px}.filters{display:flex;gap:12px;align-items:end;flex-wrap:wrap;margin:28px 0}.filters label{display:grid;gap:5px;font-size:.72rem;font-weight:900;text-transform:uppercase;letter-spacing:.1em}.filters input,.filters select{min-height:42px;padding:8px 10px;border:1px solid var(--line);background:rgba(255,255,255,.92);color:var(--ink);font:inherit}.filters input:focus,.filters select:focus{outline:2px solid rgba(20,107,85,.3);outline-offset:2px}.filters input{min-width:250px}.layout{display:grid;grid-template-columns:minmax(0,1.5fr) minmax(280px,.8fr);gap:18px}.primary,.secondary{display:grid;gap:18px;align-content:start}.panel,.empty-state{background:rgba(255,255,255,.86);border:1px solid var(--line);box-shadow:0 9px 28px rgba(19,47,38,.05)}.panel{padding:21px}.panel-heading{display:flex;justify-content:space-between;gap:18px;align-items:start;margin-bottom:15px}.panel-heading h2{font-size:1.5rem;letter-spacing:-.02em}.panel-heading p{font-size:.82rem;margin:0}.triage-list{display:grid}.test-row{display:flex;justify-content:space-between;gap:18px;padding:14px 0;border-top:1px solid var(--line)}.identity{display:flex;gap:10px;min-width:0}.identity h3{font-size:1.1rem;overflow-wrap:anywhere}.identity p,.observation small{color:var(--muted);font-size:.78rem;margin:3px 0 0;overflow-wrap:anywhere}.signal{align-self:start;font-size:.64rem;font-weight:900;letter-spacing:.1em;text-transform:uppercase;padding:4px 6px;background:#e9f4ee;color:var(--green)}.signal.regression{background:#fde9e9;color:var(--red)}.signal.flaky{background:#fff2d8;color:var(--gold)}.signal.known{background:#efecfa;color:var(--violet)}.observation{text-align:right;white-space:nowrap}.observation span{letter-spacing:.08em;font-size:.82rem}.run-list,.cluster-list{margin:0;padding:0;list-style:none}.run-list li{display:grid;grid-template-columns:minmax(0,1fr) 100px 44px;gap:10px;align-items:center;padding:11px 0;border-top:1px solid var(--line)}.run-list div:first-child{display:grid;gap:2px;min-width:0}.run-list span{color:var(--muted);font-size:.73rem}.bar{display:flex;height:8px;overflow:hidden;background:#e3ebe7}.bar i{display:block;min-width:0;height:100%}.bar .pass{background:var(--green)}.bar .skip{background:var(--gold)}.bar .fail{background:var(--red)}.cluster-list li{display:flex;justify-content:space-between;gap:14px;padding:11px 0;border-top:1px solid var(--line);font-size:.82rem}.cluster-list code{color:var(--violet)}.empty-state{margin-top:28px;padding:42px;max-width:760px}.empty-state h2{font-size:1.8rem}.empty-state p{color:var(--muted);max-width:640px}@media(max-width:840px){.hero{align-items:start;grid-template-columns:1fr}.hero-actions{justify-items:start}.hero nav{justify-content:start}.summary{grid-template-columns:repeat(2,1fr)}.stat:nth-child(3){border-left:0}.layout{grid-template-columns:1fr}.panel-heading{flex-direction:column}.test-row{align-items:start;flex-direction:column}.observation{text-align:left}}@media(max-width:520px){.shell{padding:22px 14px}.summary{grid-template-columns:1fr}.stat{border-left:0;border-top:1px solid var(--line)}.stat:first-child{border-top:0}.filters input{min-width:0;width:100%}.run-list li{grid-template-columns:minmax(0,1fr) 70px 38px}}@media(prefers-reduced-motion:no-preference){.panel,.stat{animation:rise .28s ease-out both}@keyframes rise{from{opacity:0;transform:translateY(7px)}to{opacity:1;transform:none}}}
            """ + workspaceDashboardStyles() + dashboardViewStyles();
    }

    private static String nightShiftStyles() {
        return """
            /* Autopilot is a triage surface, not a spreadsheet: use the same night-shift language as the run report. */
            :root{--ink:#edf4fb;--muted:#91a3b5;--paper:#070b11;--panel:#0d151f;--line:#27394b;--green:#5ee6b0;--red:#ff6876;--gold:#f6bf58;--violet:#c39bff;--blue:#67c7ff}body{background:#070b11;color:var(--ink);font:14px/1.5 ui-monospace,SFMono-Regular,Menlo,Consolas,monospace}body:before{content:"";position:fixed;inset:0;pointer-events:none;background-image:linear-gradient(rgba(103,199,255,.065) 1px,transparent 1px),linear-gradient(90deg,rgba(103,199,255,.065) 1px,transparent 1px);background-size:32px 32px;mask-image:linear-gradient(#000,transparent 72%)}.shell{max-width:1400px;padding:32px}
            .hero{position:relative;grid-template-columns:minmax(0,1fr) auto;gap:30px;align-items:end;padding:32px;border:1px solid var(--line);border-left:6px solid var(--blue);background:linear-gradient(110deg,#0d1722,#0a111a 68%,#101b27);box-shadow:0 22px 50px rgba(0,0,0,.38)}.hero:before{content:"AUTOPILOT / HISTORY WINDOW";position:absolute;top:12px;right:18px;color:rgba(103,199,255,.62);font-size:10px;font-weight:900;letter-spacing:.14em}.hero-copy{padding-top:12px}.hero h1{font-family:"Arial Narrow","Avenir Next Condensed",Impact,sans-serif;font-size:clamp(3.1rem,6vw,5.4rem);font-weight:800;line-height:.86;letter-spacing:-.055em;text-transform:uppercase}.hero h1,.panel-heading h2,.identity h3{color:var(--ink)}.eyebrow{color:var(--green);font-size:10px;letter-spacing:.16em}.subtitle,.muted,.panel-heading>p{color:var(--muted)}.hero-actions{gap:10px}.hero-status{min-width:270px;padding:12px 14px;border-left:4px solid var(--green);background:rgba(86,216,139,.08)}.hero-status.warn{border-left-color:var(--gold);background:rgba(246,191,88,.08)}.hero-status.fail{border-left-color:var(--red);background:rgba(255,104,118,.09)}.hero-status span,.hero-status small{color:var(--muted);font-size:10px;text-transform:uppercase;letter-spacing:.08em}.hero-status strong{color:var(--ink);font-size:15px;letter-spacing:.06em}.link-button{border:1px solid #36526b;border-radius:0;padding:9px 12px;background:var(--blue);color:#071018;font-size:11px;letter-spacing:.04em}.link-button:hover{background:var(--green);color:#071018}.link-button.quiet{background:#0d1722;color:#c7d7e5}
            .run-note{margin:15px 0;color:var(--muted);font-size:12px}.summary{gap:10px;margin:18px 0 22px;border:0;background:transparent;box-shadow:none}.stat{min-height:124px;padding:16px;border:1px solid var(--line);border-left:4px solid #52677c;background:linear-gradient(180deg,#101a25,#0b121b)}.stat:first-child{border-left:4px solid var(--green)}.stat.fail{border-left-color:var(--red)}.stat.warn{border-left-color:var(--gold)}.stat.known{border-left-color:var(--violet)}.stat:before{display:none}.stat span{color:var(--muted);font-size:10px;letter-spacing:.1em}.stat strong{font-family:"Arial Narrow","Avenir Next Condensed",Impact,sans-serif;font-size:42px;line-height:1;color:var(--ink)}.stat small{color:var(--muted)}
            .filters{gap:8px;margin:22px 0}.filters label{color:var(--muted);font-size:10px;letter-spacing:.1em}.filters input,.filters select{min-height:40px;border:1px solid #2b4054;border-radius:0;background:#0d1722;color:var(--ink)}.filters input:focus,.filters select:focus{outline-color:var(--blue)}.layout{gap:10px;grid-template-columns:minmax(0,1.6fr) minmax(290px,.9fr)}.primary,.secondary{gap:10px}.panel,.empty-state{border:1px solid var(--line);border-radius:0;background:#0d151f;box-shadow:none}.panel{padding:20px}.panel-heading{margin-bottom:16px}.panel-heading h2{font-family:"Arial Narrow","Avenir Next Condensed",Impact,sans-serif;font-size:24px;letter-spacing:.01em;text-transform:uppercase}.panel-heading p{font-size:12px}.test-row{padding:15px 0;border-top-color:#26394b}.identity h3{font-family:"Arial Narrow","Avenir Next Condensed",Impact,sans-serif;font-size:20px;letter-spacing:.01em}.identity p,.observation small{color:var(--muted)}.signal{border-radius:0;background:rgba(86,216,139,.14);color:var(--green);font-size:10px}.signal.regression{background:rgba(255,104,118,.15);color:var(--red)}.signal.flaky{background:rgba(246,191,88,.14);color:var(--gold)}.signal.known{background:rgba(195,155,255,.14);color:var(--violet)}.observation{color:#c8d8e5}.run-list li{grid-template-columns:minmax(0,1fr) 110px 44px;padding:12px 0;border-top-color:#26394b}.run-list span{color:var(--muted)}.bar{height:9px;border-radius:0;background:#1a2a39}.cluster-list li{border-top-color:#26394b;color:#c9d6e1}.cluster-list code{color:var(--violet)}.empty-state{padding:42px}.empty-state h2{font-family:"Arial Narrow","Avenir Next Condensed",Impact,sans-serif;color:var(--ink)}
            @media(max-width:840px){.hero{grid-template-columns:1fr;padding:27px 20px 20px}.hero:before{position:static;display:block;margin-bottom:18px}.hero-actions,.hero nav{justify-items:start;justify-content:start}.summary{grid-template-columns:repeat(2,1fr)}.layout{grid-template-columns:1fr}}@media(max-width:520px){.shell{padding:14px}.summary{grid-template-columns:1fr}.filters input{width:100%}.run-list li{grid-template-columns:minmax(0,1fr) 72px 36px}}
            """;
    }

    private static String releaseDispatchStyles() {
        return """
            /* Reliability dispatch: make the history useful for a decision before exposing its inventory. */
            :root{--ink:#12203e;--muted:#63718b;--paper:#f4f0e8;--panel:#fffdf8;--line:#cfd5df;--green:#147a62;--red:#df3b32;--gold:#c17d13;--violet:#7b43c4;--blue:#2848d8}body{background:linear-gradient(90deg,rgba(40,72,216,.08) 1px,transparent 1px),linear-gradient(#f4f0e8,#ede7dc);background-size:80px 80px;color:var(--ink);font:15px/1.5 "Avenir Next",Avenir,"Helvetica Neue",sans-serif}body:before{display:none}.shell{max-width:1400px;padding:38px 32px 84px}
            .hero{grid-template-columns:minmax(0,1.3fr) auto;gap:32px;align-items:end;padding:36px 38px 30px;border:1px solid var(--ink);border-top:14px solid var(--blue);border-left:1px solid var(--ink);background:var(--panel);box-shadow:12px 12px 0 rgba(18,32,62,.13)}.hero:before{content:"AUTOPILOT / LOCAL HISTORY";top:18px;right:24px;color:var(--blue);font:800 10px/1 "Avenir Next",sans-serif;letter-spacing:.13em}.hero-copy{padding-top:16px}.hero h1{font-family:Georgia,"Times New Roman",serif;font-size:clamp(3.25rem,6.5vw,6rem);font-weight:800;line-height:.83;letter-spacing:-.07em;text-transform:none}.eyebrow{color:var(--blue);font-size:11px;letter-spacing:.16em}.subtitle,.muted,.panel-heading>p{color:var(--muted)}.hero-status{min-width:290px;padding:12px 14px;border-left:0;border-top:3px solid var(--green);background:#e7f5ef}.hero-status.warn{border-top-color:var(--gold);background:#fff5dd}.hero-status.fail{border-top-color:var(--red);background:#ffe8e5}.hero-status span,.hero-status small{color:var(--muted);font-size:10px}.hero-status strong{color:var(--ink);font-family:Georgia,serif;font-size:18px;text-transform:none;letter-spacing:0}.link-button{border:1px solid var(--ink);border-radius:0;background:var(--blue);color:#fff;font-size:11px;letter-spacing:.04em}.link-button:hover{background:var(--ink);color:#fff}.link-button.quiet{background:#fff;color:var(--blue)}
            .run-note{margin:15px 0;color:var(--muted);font-size:12px}.summary{display:none}.history-brief{display:grid;grid-template-columns:.9fr 1.15fr .95fr;gap:14px;margin:22px 0}.history-brief article{min-width:0}.window-card{display:grid;align-content:start;gap:6px;padding:22px;border:1px solid var(--ink);background:var(--ink);color:#fff}.window-card p,.attention-card>p{margin:0;font-size:10px;font-weight:800;letter-spacing:.14em;text-transform:uppercase}.window-card p{color:#bdc7da}.window-card strong{font-family:Georgia,serif;font-size:43px;line-height:1;letter-spacing:-.06em}.window-card span,.window-card small{color:#d2dbeb;font-size:12px}.stability-card{padding:22px;border:1px solid var(--ink);background:#e6ff56}.stability-card>span{font-size:10px;font-weight:800;letter-spacing:.14em;text-transform:uppercase}.stability-card strong{display:block;margin:6px 0 8px;font-family:Georgia,serif;font-size:clamp(3.2rem,5vw,5.4rem);line-height:.76;letter-spacing:-.08em}.stability-card strong em{font-size:.34em;font-style:normal}.stability-card p{margin:0 0 14px;color:#405029;font-size:12px}.stability-card .bar{height:10px;border-radius:0;background:#b3ca3c}.stability-card .bar .pass{background:var(--green)}.stability-card .bar .skip{background:var(--gold)}.stability-card .bar .fail{background:var(--red)}.attention-card{padding:14px 18px;border:1px solid var(--ink);background:var(--panel)}.attention-card>p{margin-bottom:8px;color:var(--blue)}.attention-card div{display:flex;justify-content:space-between;gap:10px;padding:8px 0;border-top:1px solid var(--line);font-size:13px}.attention-card b{font-family:Georgia,serif;font-size:19px}.attention-card div:nth-child(2) b{color:var(--red)}.attention-card div:nth-child(3) b{color:var(--gold)}.attention-card div:nth-child(4) b{color:var(--violet)}
            .filters{gap:8px;margin:22px 0}.filters label{font-size:10px;letter-spacing:.1em;color:var(--muted)}.filters input,.filters select{min-height:42px;border:1px solid var(--ink);border-radius:0;background:#fff;color:var(--ink)}.filters input:focus,.filters select:focus{outline-color:var(--blue)}.layout{gap:14px;grid-template-columns:minmax(0,1.5fr) minmax(300px,.8fr)}.primary,.secondary{gap:14px}.panel,.empty-state{border:1px solid var(--ink);border-radius:0;background:var(--panel);box-shadow:5px 5px 0 rgba(18,32,62,.09)}.panel{padding:22px}.panel:nth-child(1){background:#fff0ed}.panel-heading h2{font-family:Georgia,serif;font-size:24px;letter-spacing:-.04em;text-transform:none}.panel-heading p{font-size:12px}.test-row{padding:15px 0;border-top-color:var(--line)}.identity h3{font-family:Georgia,serif;font-size:20px;letter-spacing:-.03em}.identity p,.observation small{color:var(--muted)}.signal{border-radius:0;background:#e3f6ec;color:var(--green);font-size:10px}.signal.regression{background:#ffe6e2;color:var(--red)}.signal.flaky{background:#fff2d4;color:var(--gold)}.signal.known{background:#eee4ff;color:var(--violet)}.observation{color:var(--ink)}.run-list li{grid-template-columns:minmax(0,1fr) 110px 44px;border-top-color:var(--line)}.run-list span{color:var(--muted)}.bar{border-radius:0;background:#dfe5ec}.cluster-list li{border-top-color:var(--line);color:var(--ink)}.cluster-list code{color:var(--violet)}.empty-state{padding:42px}.empty-state h2{font-family:Georgia,serif;color:var(--ink)}
            @media(max-width:840px){.hero{grid-template-columns:1fr;padding:28px 22px 22px}.hero:before{position:static;display:block;margin-bottom:18px}.hero-actions,.hero nav{justify-items:start;justify-content:start}.history-brief{grid-template-columns:1fr 1fr}.stability-card{grid-column:span 2}.layout{grid-template-columns:1fr}}@media(max-width:520px){.shell{padding:18px 14px 54px}.hero h1{font-size:clamp(3.4rem,17vw,5.2rem)}.history-brief{grid-template-columns:1fr}.stability-card{grid-column:auto}.filters input{width:100%}.run-list li{grid-template-columns:minmax(0,1fr) 72px 36px}}
            """;
    }

    private static String productDashboardStyles() {
        return """
            :root{--ink:#1f2328;--muted:#656d76;--paper:#f6f8fa;--panel:#fff;--line:#d0d7de;--green:#1a7f37;--red:#cf222e;--gold:#9a6700;--violet:#8250df;--blue:#0969da}body{background:var(--paper);color:var(--ink);font:14px/1.5 -apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}body:before{display:none}.shell{max-width:1280px;padding:32px 24px 56px}
            .hero{position:static;grid-template-columns:minmax(0,1fr) auto;gap:24px;align-items:end;padding:24px;border:1px solid var(--line);border-top:1px solid var(--line);border-left:1px solid var(--line);background:var(--panel);box-shadow:0 1px 2px rgba(31,35,40,.08)}.hero:before{display:none}.hero-copy{padding:0}.hero h1{font-family:inherit;font-size:30px;font-weight:650;line-height:1.2;letter-spacing:-.02em;text-transform:none}.eyebrow{color:var(--muted);font-size:12px;letter-spacing:0;text-transform:none}.subtitle,.muted,.panel-heading>p{color:var(--muted)}.hero-actions{gap:10px}.hero-status{min-width:250px;padding:10px 12px;border-left:4px solid var(--green);border-top:0;background:#dafbe1}.hero-status.warn{border-left-color:var(--gold);background:#fff8c5}.hero-status.fail{border-left-color:var(--red);background:#ffebe9}.hero-status span,.hero-status small{color:var(--muted);font-size:11px;text-transform:none;letter-spacing:0}.hero-status strong{color:var(--ink);font-family:inherit;font-size:14px;font-weight:650;text-transform:none;letter-spacing:0}.link-button{border:1px solid var(--line);border-radius:6px;background:var(--blue);color:#fff;font-size:12px;letter-spacing:0}.link-button:hover{background:#0550ae;color:#fff}.link-button.quiet{background:var(--panel);color:var(--brand)}
            .run-note{margin:14px 0;color:var(--muted);font-size:12px}.summary{display:none}.history-brief{grid-template-columns:minmax(200px,.85fr) minmax(250px,1.1fr) minmax(210px,.9fr);gap:16px;margin:16px 0}.history-brief article{border:1px solid var(--line);border-radius:8px;background:var(--panel);box-shadow:0 1px 2px rgba(31,35,40,.08)}.window-card{gap:4px;padding:16px;color:var(--ink)}.window-card p,.attention-card>p{margin:0;color:var(--muted);font-size:12px;font-weight:600;letter-spacing:0;text-transform:none}.window-card strong{font-family:inherit;font-size:28px;font-weight:650;letter-spacing:-.02em}.window-card span,.window-card small{color:var(--muted);font-size:12px}.stability-card{padding:16px;background:var(--panel)}.stability-card>span{color:var(--muted);font-size:12px;font-weight:600;letter-spacing:0;text-transform:none}.stability-card strong{margin:5px 0 6px;font-family:inherit;font-size:38px;font-weight:650;line-height:1;letter-spacing:-.03em}.stability-card strong em{font-size:.55em;font-style:normal}.stability-card p{margin:0 0 10px;color:var(--muted);font-size:12px}.stability-card .bar{height:8px;border-radius:999px;background:#d8dee4}.stability-card .bar .pass{background:var(--green)}.stability-card .bar .skip{background:var(--gold)}.stability-card .bar .fail{background:var(--red)}.attention-card{padding:12px 16px}.attention-card>p{margin-bottom:6px;color:var(--muted)}.attention-card div{padding:6px 0;border-top:1px solid var(--line);font-size:13px}.attention-card b{font-family:inherit;font-size:16px;font-weight:650}.attention-card div:nth-child(2) b{color:var(--red)}.attention-card div:nth-child(3) b{color:var(--gold)}.attention-card div:nth-child(4) b{color:var(--violet)}
            .filters{gap:10px;margin:18px 0}.filters label{color:var(--muted);font-size:12px;letter-spacing:0}.filters input,.filters select{min-height:36px;border:1px solid var(--line);border-radius:6px;background:var(--panel);color:var(--ink)}.filters input:focus-visible,.filters select:focus-visible{outline:2px solid var(--blue);outline-offset:2px}.layout{gap:16px;grid-template-columns:minmax(0,1.5fr) minmax(280px,.8fr)}.primary,.secondary{gap:16px}.panel,.empty-state{border:1px solid var(--line);border-radius:8px;background:var(--panel);box-shadow:0 1px 2px rgba(31,35,40,.08)}.panel{padding:18px}.panel:nth-child(1){background:var(--panel)}.panel-heading{margin-bottom:14px}.panel-heading h2{font-family:inherit;font-size:16px;font-weight:650;letter-spacing:0;text-transform:none}.panel-heading p{font-size:12px}.test-row{padding:13px 0;border-top-color:var(--line)}.identity h3{font-family:inherit;font-size:15px;font-weight:650;letter-spacing:0}.identity p,.observation small{color:var(--muted)}.signal{border-radius:999px;background:#dafbe1;color:var(--green);font-size:10px;letter-spacing:0}.signal.regression{background:#ffebe9;color:var(--red)}.signal.flaky{background:#fff8c5;color:var(--gold)}.signal.known{background:#fbefff;color:var(--violet)}.observation{color:var(--ink)}.run-list li{grid-template-columns:minmax(0,1fr) 100px 44px;border-top-color:var(--line)}.run-list span{color:var(--muted)}.bar{border-radius:999px;background:#d8dee4}.cluster-list li{border-top-color:var(--line);color:var(--ink)}.cluster-list code{color:var(--violet)}.empty-state{padding:36px}.empty-state h2{font-family:inherit;color:var(--ink)}
            @media(max-width:840px){.hero{grid-template-columns:1fr;padding:20px}.hero-actions,.hero nav{justify-items:start;justify-content:start}.history-brief{grid-template-columns:1fr 1fr}.stability-card{grid-column:span 2}.layout{grid-template-columns:1fr}}@media(max-width:520px){.shell{padding:16px}.hero h1{font-size:26px}.history-brief{grid-template-columns:1fr}.stability-card{grid-column:auto}.filters input{width:100%}.run-list li{grid-template-columns:minmax(0,1fr) 72px 36px}}
            """;
    }

    private static String workspaceDashboardStyles() {
        return """
            :root{--ink:#1f2328;--muted:#656d76;--paper:#f6f8fa;--panel:#fff;--line:#d0d7de;--green:#1a7f37;--red:#cf222e;--gold:#9a6700;--violet:#8250df;--blue:#0969da}body{background:var(--paper);color:var(--ink);font:14px/1.5 -apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}body:before{display:none}.shell{max-width:1280px;padding:32px 24px 56px}.hero{position:static;display:grid;grid-template-columns:minmax(0,1fr) auto;gap:24px;align-items:center;padding:24px;border:1px solid var(--line);border-radius:8px;background:var(--panel);box-shadow:0 1px 2px rgba(31,35,40,.08)}.hero:before{display:none}.hero-copy{min-width:0;padding:0}.hero h1{margin:0;font-family:inherit;font-size:28px;font-weight:650;line-height:1.25;letter-spacing:-.02em;text-transform:none}.eyebrow{margin:0 0 4px;color:var(--muted);font-size:12px;font-weight:500;letter-spacing:0;text-transform:none}.subtitle,.muted,.panel-heading>p{color:var(--muted)}.subtitle{margin:6px 0 0;max-width:620px}.hero-actions{display:grid;justify-items:end;gap:10px;min-width:250px}.hero nav{display:flex;justify-content:flex-end;gap:8px}.hero-status{width:100%;min-width:0;padding:10px 12px;border-left:4px solid var(--green);border-top:0;background:#dafbe1}.hero-status.warn{border-left-color:var(--gold);background:#fff8c5}.hero-status.fail{border-left-color:var(--red);background:#ffebe9}.hero-status span,.hero-status small{color:var(--muted);font-size:11px;text-transform:none;letter-spacing:0}.hero-status strong{color:var(--ink);font-family:inherit;font-size:14px;font-weight:650;text-transform:none;letter-spacing:0}.link-button{border:1px solid var(--line);border-radius:6px;padding:7px 10px;background:var(--blue);color:#fff;font-size:12px;font-weight:600;text-decoration:none}.link-button:hover{background:#0550ae;color:#fff}.link-button.quiet{background:var(--panel);color:var(--blue)}.run-note{margin:12px 0 0;color:var(--muted);font-size:12px}
            .summary{display:none}.history-brief{display:grid;grid-template-columns:minmax(220px,.9fr) minmax(220px,1fr) minmax(220px,.9fr);gap:12px;margin:16px 0}.history-brief article{min-width:0;border:1px solid var(--line);border-radius:8px;background:var(--panel);box-shadow:0 1px 2px rgba(31,35,40,.08)}.window-card{display:grid;align-content:start;gap:4px;padding:16px;color:var(--ink)}.window-card p,.attention-card>p{margin:0;color:var(--muted);font-size:12px;font-weight:600;letter-spacing:0;text-transform:none}.window-card strong{font-family:inherit;font-size:27px;font-weight:650;letter-spacing:-.02em}.window-card span,.window-card small{overflow-wrap:anywhere;color:var(--muted);font-size:12px}.stability-card{padding:16px}.stability-card>span{color:var(--muted);font-size:12px;font-weight:600;letter-spacing:0;text-transform:none}.stability-card strong{display:block;margin:3px 0 5px;font-family:inherit;font-size:34px;font-weight:650;line-height:1;letter-spacing:-.03em}.stability-card strong em{font-size:.55em;font-style:normal}.stability-card p{margin:0 0 10px;color:var(--muted);font-size:12px}.stability-card .bar{height:7px;border-radius:999px;background:#d8dee4}.stability-card .bar .pass{background:var(--green)}.stability-card .bar .skip{background:var(--gold)}.stability-card .bar .fail{background:var(--red)}.attention-card{padding:12px 16px}.attention-card>p{margin-bottom:5px}.attention-card div{display:flex;justify-content:space-between;gap:12px;padding:5px 0;border-top:1px solid var(--line);font-size:13px}.attention-card b{font-family:inherit;font-size:15px;font-weight:650}.attention-card div:nth-child(2) b{color:var(--red)}.attention-card div:nth-child(3) b{color:var(--gold)}.attention-card div:nth-child(4) b{color:var(--violet)}
            .trend-panel{margin:0 0 16px;padding:18px;border:1px solid var(--line);border-radius:8px;background:var(--panel);box-shadow:0 1px 2px rgba(31,35,40,.08)}.trend-heading{display:flex;justify-content:space-between;gap:18px;align-items:baseline}.trend-heading h2{margin:0;font-size:16px;font-weight:650;letter-spacing:-.01em}.trend-heading p{margin:4px 0 0;color:var(--muted);font-size:12px}.trend-heading>span{color:var(--muted);font-size:12px;white-space:nowrap}.trend-chart{display:grid;grid-auto-columns:minmax(42px,1fr);grid-auto-flow:column;gap:12px;align-items:end;min-height:194px;margin:16px 0 8px;padding:0;list-style:none}.trend-chart li{display:grid;grid-template-rows:auto 144px auto;gap:5px;min-width:0;text-align:center}.trend-chart li>strong{font-size:12px;font-weight:600;font-variant-numeric:tabular-nums}.trend-stack{display:flex;flex-direction:column-reverse;height:144px;overflow:hidden;border:1px solid var(--line);border-radius:5px;background:#f6f8fa}.trend-stack i{display:block;flex:0 0 auto;min-height:0}.trend-stack .pass{background:#54ae6b}.trend-stack .skip{background:#d4a72c}.trend-stack .fail{background:#e5534b}.trend-chart li>span{overflow:hidden;color:var(--muted);font-size:10px;line-height:1.2;text-overflow:ellipsis;white-space:nowrap}.trend-legend{display:flex;gap:14px;color:var(--muted);font-size:11px}.trend-legend span{display:inline-flex;align-items:center;gap:5px}.trend-legend span:before{content:"";width:8px;height:8px;border-radius:2px;background:currentColor}.trend-legend .pass{color:var(--green)}.trend-legend .skip{color:var(--gold)}.trend-legend .fail{color:var(--red)}
            .filters{display:flex;gap:10px;align-items:end;flex-wrap:wrap;margin:16px 0}.filters label{display:grid;gap:4px;color:var(--muted);font-size:12px;font-weight:500;letter-spacing:0}.filters input,.filters select{min-height:36px;border:1px solid var(--line);border-radius:6px;background:var(--panel);color:var(--ink)}.filters input:focus-visible,.filters select:focus-visible{outline:2px solid var(--blue);outline-offset:2px}.layout{display:grid;grid-template-columns:minmax(0,1.55fr) minmax(280px,.75fr);gap:16px}.primary,.secondary{display:grid;gap:16px;align-content:start;min-width:0}.panel,.empty-state{border:1px solid var(--line);border-radius:8px;background:var(--panel);box-shadow:0 1px 2px rgba(31,35,40,.08)}.panel{padding:18px}.panel-heading{display:flex;justify-content:space-between;gap:12px;align-items:baseline;margin-bottom:12px}.panel-heading h2{margin:0;font-family:inherit;font-size:16px;font-weight:650;letter-spacing:-.01em;text-transform:none}.panel-heading>span{color:var(--muted);font-size:12px;font-weight:600;font-variant-numeric:tabular-nums}.test-row{padding:13px 0;border-top-color:var(--line)}.identity h3{font-family:inherit;font-size:15px;font-weight:650;letter-spacing:0}.identity p,.observation small{color:var(--muted)}.signal{border-radius:999px;background:#dafbe1;color:var(--green);font-size:10px;letter-spacing:0}.signal.regression{background:#ffebe9;color:var(--red)}.signal.flaky{background:#fff8c5;color:var(--gold)}.signal.known{background:#fbefff;color:var(--violet)}.observation{color:var(--ink)}.cluster-list li{border-top-color:var(--line);color:var(--ink)}.cluster-list code{color:var(--violet)}.empty-state{padding:36px}.empty-state h2{font-family:inherit;color:var(--ink)}
            @media(max-width:840px){.hero{grid-template-columns:1fr;align-items:start}.hero-actions{justify-items:start;min-width:0;width:100%}.hero nav{justify-content:start}.history-brief{grid-template-columns:1fr 1fr}.stability-card{grid-column:span 2}.layout{grid-template-columns:1fr}}@media(max-width:560px){.shell{padding:16px}.hero h1{font-size:25px}.history-brief{grid-template-columns:1fr}.stability-card{grid-column:auto}.filters input{width:100%}.trend-panel{padding:14px;overflow-x:auto}.trend-chart{min-width:480px}.trend-heading{align-items:start;flex-direction:column;gap:4px}}
            """;
    }

    private static String dashboardViewStyles() {
        return """
            .view-tabs{display:flex;gap:4px;margin:16px 0 0;border-bottom:1px solid var(--line)}.view-tabs button{margin:0 0 -1px;padding:9px 12px;border:1px solid transparent;border-bottom:0;border-radius:6px 6px 0 0;background:transparent;color:var(--muted);font:600 13px/1.2 -apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;cursor:pointer}.view-tabs button[aria-selected="true"]{border-color:var(--line);background:var(--panel);color:var(--ink)}.view-tabs button:hover{color:var(--ink)}.view-tabs button:focus-visible{outline:2px solid var(--blue);outline-offset:2px}.dashboard-view[hidden]{display:none!important}.dashboard-view>.history-brief{margin-top:16px}.panel-description{margin:-4px 0 12px;color:var(--muted);font-size:12px}
            """;
    }

    private static String script() {
        return """
            const search=document.getElementById('search'),task=document.getElementById('taskFilter'),status=document.getElementById('statusFilter');
            if(search){const apply=()=>document.querySelectorAll('.test-row').forEach(row=>{const text=row.dataset.search.toLowerCase(),matchesText=text.includes(search.value.toLowerCase()),matchesTask=task.value==='all'||row.dataset.task===task.value,matchesStatus=status.value==='all'||row.dataset.status===status.value;row.hidden=!(matchesText&&matchesTask&&matchesStatus)});[search,task,status].forEach(control=>control.addEventListener('input',apply));}
            const tabs=[...document.querySelectorAll('[role="tab"]')];
            const activate=tab=>{tabs.forEach(item=>{const active=item===tab;item.setAttribute('aria-selected',active);item.tabIndex=active?0:-1;document.getElementById(item.getAttribute('aria-controls')).hidden=!active;});tab.focus();};
            tabs.forEach((tab,index)=>tab.addEventListener('keydown',event=>{if(!['ArrowLeft','ArrowRight','Home','End'].includes(event.key))return;event.preventDefault();const target=event.key==='Home'?tabs[0]:event.key==='End'?tabs[tabs.length-1]:tabs[(index+(event.key==='ArrowRight'?1:tabs.length-1))%tabs.length];activate(target); }));
            tabs.forEach(tab=>tab.addEventListener('click',()=>activate(tab)));
            """;
    }

    private static final class RunSummary {
        private final String taskName;
        private final String timestamp;
        private final int total;
        private final int passed;
        private final int skipped;
        private final int failed;
        private RunSummary(String taskName, String timestamp, int total, int passed, int skipped, int failed) { this.taskName = taskName; this.timestamp = timestamp; this.total = total; this.passed = passed; this.skipped = skipped; this.failed = failed; }
        private static RunSummary from(RunHistoryStore.HistoryRun run) {
            int passed = (int) run.tests().stream().filter(test -> isPass(test.status())).count();
            int failed = (int) run.tests().stream().filter(test -> isFailure(test.status())).count();
            int skipped = Math.max(0, run.tests().size() - passed - failed);
            return new RunSummary(run.displayTask(), run.timestamp(), run.tests().size(), passed, skipped, failed);
        }
        private int passRate() { return total == 0 ? 0 : (int) Math.round(passed * 100.0 / total); }
        private int pct(int count) { return total == 0 ? 0 : (int) Math.round(count * 100.0 / total); }
        private String healthKind() { return failed > 0 ? "fail" : skipped > 0 ? "warn" : "pass"; }
        private String healthLabel() { return failed > 0 ? "Needs attention" : skipped > 0 ? "Stable with skips" : "All clear"; }
        private String displayTime() { try { return DATE.format(Instant.parse(timestamp)); } catch (Exception ignored) { return timestamp == null || timestamp.isBlank() ? "unknown time" : timestamp; } }
    }

    private static final class TestTimeline {
        private final List<Observation> observations = new ArrayList<>();
        private String taskName = "Unknown task";
        private String testName = "";
        private String className = "";
        private void add(RunHistoryStore.HistoryRun run, RunHistoryStore.HistoryTest test) {
            taskName = run.displayTask();
            if (!test.testName().isBlank()) testName = test.testName();
            if (!test.className().isBlank()) className = test.className();
            observations.add(new Observation(run.timestamp(), test.status(), test.fingerprint(), test));
        }
        private boolean isRegression() { return observations.size() >= 2 && isFailure(lastStatus()) && isPass(observations.get(observations.size() - 2).status); }
        private boolean isFlaky() { List<Observation> recent = observations.subList(Math.max(0, observations.size() - 10), observations.size()); return recent.stream().filter(o -> isPass(o.status)).count() >= 2 && recent.stream().filter(o -> isFailure(o.status)).count() >= 2; }
        private boolean isKnownFailure() { return isFailure(lastStatus()) && !last().fingerprint.isBlank() && matchingFailures() >= 3; }
        private int matchingFailures() { return (int) observations.stream().filter(o -> isFailure(o.status) && o.fingerprint.equals(last().fingerprint)).count(); }
        private int failures() { return (int) observations.stream().filter(o -> isFailure(o.status)).count(); }
        private String lastStatus() { return observations.isEmpty() ? "" : last().status; }
        private String lastTimestamp() { return observations.isEmpty() ? "" : last().timestamp; }
        private Observation last() { return observations.get(observations.size() - 1); }
        private String displayName() { return testName.isBlank() ? last().test.displayName() : testName; }
        private String displayClass() { return className.isBlank() ? last().test.displayClass() : className; }
        private String sequence() { return observations.stream().skip(Math.max(0, observations.size() - 8)).map(o -> isPass(o.status) ? "P" : isFailure(o.status) ? "F" : "S").collect(Collectors.joining(" ")); }
        private String searchText() { return displayName() + " " + displayClass() + " " + taskName; }
    }

    private record Observation(String timestamp, String status, String fingerprint, RunHistoryStore.HistoryTest test) {}
    private static final class FingerprintCluster {
        private final String fingerprint; private int failures; private final Set<String> tests = new TreeSet<>();
        private FingerprintCluster(String fingerprint) { this.fingerprint = fingerprint; }
        private void add(RunHistoryStore.HistoryTest test) { failures++; tests.add(test.displayClass() + "." + test.displayName()); }
        private String shortFingerprint() { return fingerprint.length() <= 12 ? fingerprint : fingerprint.substring(0, 12); }
    }
}
