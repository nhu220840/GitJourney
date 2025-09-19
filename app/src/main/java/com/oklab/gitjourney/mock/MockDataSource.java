package com.oklab.gitjourney.mock;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import com.oklab.gitjourney.data.ActionType;
import com.oklab.gitjourney.data.ContributionDataEntry;
import com.oklab.gitjourney.data.FeedDataEntry;
import com.oklab.gitjourney.data.GitHubJourneyWidgetDataEntry;
import com.oklab.gitjourney.data.GitHubRepoContentType;
import com.oklab.gitjourney.data.GitHubUserProfileDataEntry;
import com.oklab.gitjourney.data.RepositoryContentDataEntry;
import com.oklab.gitjourney.data.UserSessionData;
import com.oklab.gitjourney.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralised factory for producing deterministic yet random looking mock data that mimics
 * GitHub responses.
 */
public final class MockDataSource {
    private static final String TAG = MockDataSource.class.getSimpleName();
    public static final String MOCK_HOST = "mock.gitjourney";
    private static final String ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final int ITEMS_PER_PAGE = 10;
    private static final String[] FIRST_NAMES = {
            "Linh", "Bao", "Minh", "Anh", "Khanh", "Trang", "Dung", "Long", "Lan", "Vy",
            "Tuan", "An", "Huy", "Thao", "Son", "Hanh", "Nhi", "Phuc", "Tam", "Quynh"
    };
    private static final String[] LAST_NAMES = {
            "Nguyen", "Tran", "Le", "Pham", "Huynh", "Hoang", "Phan", "Vo", "Dang", "Bui",
            "Do", "Trinh", "Duong", "Vu", "Ngo", "Dao", "Mai", "Ly", "To", "Cao"
    };
    private static final String[] LANGUAGES = {
            "Kotlin", "Java", "TypeScript", "Python", "Go", "Rust", "Swift", "Dart",
            "Ruby", "C#", "C++"
    };
    private static final String[] LOCATIONS = {
            "Ha Noi, Viet Nam", "Da Nang, Viet Nam", "Ho Chi Minh, Viet Nam",
            "Singapore", "Kuala Lumpur, Malaysia", "Bangkok, Thailand",
            "Seoul, South Korea", "Tokyo, Japan", "Berlin, Germany", "Toronto, Canada"
    };
    private static final String[] COMPANY_SUFFIX = {
            "Labs", "Studios", "Tech", "Solutions", "Systems", "Analytics",
            "Works", "Dynamics", "Collective", "Garage"
    };
    private static final String[] TOPICS = {
            "companion", "tracker", "dashboard", "analytics", "automation",
            "notebook", "builder", "playground", "manager", "orchestrator",
            "pipeline", "monitor", "gateway", "explorer", "assistant"
    };
    private static final String[] ACTION_WORDS = {
            "refreshed", "polished", "boosted", "streamlined", "enhanced",
            "refactored", "stabilised", "rewired", "secured", "documented"
    };
    private static final Map<String, GitHubUserProfileDataEntry> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, MockRepository> REPOSITORY_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<MockContribution>> CONTRIBUTION_CACHE = new ConcurrentHashMap<>();

    private MockDataSource() {
    }

    public static UserSessionData ensureMockSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Utils.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String stored = prefs.getString("userSessionData", null);
        UserSessionData sessionData = UserSessionData.createUserSessionDataFromString(stored);
        if (sessionData != null) {
            return sessionData;
        }
        sessionData = createSessionForLogin(randomLogin(randomFor("session")));
        prefs.edit().putString("userSessionData", sessionData.toString()).apply();
        return sessionData;
    }

    public static UserSessionData createSessionForLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            login = "mockuser";
        }
        String trimmedLogin = login.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");
        if (trimmedLogin.isEmpty()) {
            trimmedLogin = "mockuser";
        }
        Random random = randomFor("session-" + trimmedLogin);
        String id = Integer.toString(Math.abs(trimmedLogin.hashCode()));
        String token = generateHexToken(random, 32);
        String credentials = Base64.encodeToString((trimmedLogin + ":" + token).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        return new UserSessionData(id, credentials, token, trimmedLogin);
    }

    public static GitHubUserProfileDataEntry getUserProfile(String login) {
        Random random = randomFor("profile-" + login);
        return ensureProfile(login, random);
    }

    public static JSONObject buildUserProfileJson(String login) throws JSONException {
        GitHubUserProfileDataEntry profile = getUserProfile(login);
        JSONObject json = new JSONObject();
        json.put("login", profile.getLogin());
        json.put("avatar_url", profile.getImageUri());
        json.put("url", profile.getProfileUri());
        json.put("location", profile.getLocation());
        json.put("name", profile.getName());
        json.put("company", profile.getCompany());
        json.put("blog", profile.getBlogURI());
        json.put("email", profile.getEmail());
        json.put("bio", profile.getBio());
        json.put("public_repos", profile.getPublicRepos());
        json.put("total_private_repos", Math.max(0, profile.getPublicRepos() / 4));
        json.put("public_gists", profile.getPublicGists());
        json.put("followers", profile.getFollowers());
        json.put("following", profile.getFollowing());
        json.put("created_at", formatIso(profile.getCreatedAt()));
        return json;
    }

    public static JSONArray buildRepositoriesJson(String ownerLogin, int page) throws JSONException {
        Random random = randomFor("repos-" + ownerLogin + "-" + page);
        JSONArray array = new JSONArray();
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            MockRepository repo = ensureRepository(ownerLogin, randomRepoName(random, page, i), random);
            array.put(repo.toJson());
        }
        return array;
    }

    public static JSONArray buildStarredRepositoriesJson(int page) throws JSONException {
        Random random = randomFor("stars-" + page);
        JSONArray array = new JSONArray();
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            String owner = randomLogin(random);
            MockRepository repo = ensureRepository(owner, randomRepoName(random, page, i), random);
            array.put(repo.toStarredJson());
        }
        return array;
    }

    public static JSONArray buildFollowersJson(int page) throws JSONException {
        Random random = randomFor("followers-" + page);
        JSONArray array = new JSONArray();
        if (page > 3) {
            return array;
        }
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            String login = randomLogin(random) + "f" + page + i;
            GitHubUserProfileDataEntry profile = ensureProfile(login, random);
            JSONObject obj = new JSONObject();
            obj.put("login", profile.getLogin());
            obj.put("avatar_url", profile.getImageUri());
            obj.put("url", profile.getProfileUri());
            array.put(obj);
        }
        return array;
    }

    public static JSONArray buildFollowingJson(int page) throws JSONException {
        Random random = randomFor("following-" + page);
        JSONArray array = new JSONArray();
        if (page > 3) {
            return array;
        }
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            String login = randomLogin(random) + "g" + page + i;
            GitHubUserProfileDataEntry profile = ensureProfile(login, random);
            JSONObject obj = new JSONObject();
            obj.put("login", profile.getLogin());
            obj.put("avatar_url", profile.getImageUri());
            obj.put("url", profile.getProfileUri());
            array.put(obj);
        }
        return array;
    }

    public static JSONArray buildEventsJson(String login, int page) throws JSONException {
        List<MockContribution> contributions = getContributionEntries(login, page);
        JSONArray array = new JSONArray();
        for (MockContribution contribution : contributions) {
            array.put(contribution.toJson());
        }
        return array;
    }

    public static JSONObject buildRepoReadmeJson(String owner, String repo) throws JSONException {
        MockRepository repository = ensureRepository(owner, repo, randomFor("repo-" + owner + repo));
        JSONObject json = new JSONObject();
        json.put("name", "README.md");
        json.put("download_url", repository.buildDownloadUrl("README.md"));
        json.put("type", "file");
        return json;
    }

    public static JSONArray buildRepoContentJson(String owner, String repo, String path) throws JSONException {
        MockRepository repository = ensureRepository(owner, repo, randomFor("repo-" + owner + repo));
        List<RepositoryContentDataEntry> entries = repository.getEntries(path);
        JSONArray array = new JSONArray();
        for (RepositoryContentDataEntry entry : entries) {
            JSONObject obj = new JSONObject();
            obj.put("name", entry.getName());
            obj.put("path", entry.getPath());
            obj.put("type", entry.getType() == GitHubRepoContentType.FILE ? "file" : entry.getType() == GitHubRepoContentType.DIR ? "dir" : "file");
            obj.put("download_url", entry.getUri() == null ? "" : entry.getUri());
            array.put(obj);
        }
        return array;
    }

    public static String getFileContent(String owner, String repo, String path) {
        MockRepository repository = ensureRepository(owner, repo, randomFor("repo-" + owner + repo));
        return repository.getFileContent(path);
    }

    public static List<FeedDataEntry> getFeedEntries(int page) {
        Random random = randomFor("feed-" + page);
        List<FeedDataEntry> entries = new ArrayList<>();
        Calendar now = Calendar.getInstance();
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            String author = randomPersonName(random);
            String login = author.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");
            ensureProfile(login, random);
            Calendar entryDate = (Calendar) now.clone();
            entryDate.add(Calendar.HOUR_OF_DAY, -(page * 6 + i));
            ActionType actionType = pickFeedAction(random);
            String repoName = randomRepoName(random, page, i);
            String title = actionType.name() + " in " + repoName;
            String description = "User " + author + " " + ACTION_WORDS[random.nextInt(ACTION_WORDS.length)]
                    + " the " + repoName + " repository.";
            entries.add(new FeedDataEntry(
                    buildStableId(page, i),
                    author,
                    avatarUrl(random),
                    "https://github.com/" + login,
                    title,
                    description,
                    actionType,
                    entryDate
            ));
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    public static List<GitHubJourneyWidgetDataEntry> getWidgetFeedEntries(int page) {
        Random random = randomFor("widget-feed-" + page);
        List<GitHubJourneyWidgetDataEntry> entries = new ArrayList<>();
        Calendar now = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat(ISO_PATTERN, Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            String author = randomPersonName(random);
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.HOUR_OF_DAY, -(page * 3 + i));
            String dateText = formatter.format(date.getTime());
            String repoName = randomRepoName(random, page, i);
            entries.add(new GitHubJourneyWidgetDataEntry(
                    author,
                    Uri.parse(avatarUrl(random)),
                    "Activity on " + repoName,
                    "Latest update pushed to " + repoName,
                    dateText
            ));
        }
        return entries;
    }

    public static List<ContributionDataEntry> getContributions(String login, int page) {
        List<MockContribution> contributions = getContributionEntries(login, page);
        List<ContributionDataEntry> result = new ArrayList<>(contributions.size());
        for (MockContribution contribution : contributions) {
            result.add(contribution.dataEntry);
        }
        return result;
    }

    private static GitHubUserProfileDataEntry ensureProfile(String login, Random random) {
        return PROFILE_CACHE.computeIfAbsent(login, key -> createProfile(key, random));
    }

    private static GitHubUserProfileDataEntry createProfile(String login, Random random) {
        String firstName = FIRST_NAMES[Math.abs(random.nextInt(FIRST_NAMES.length))];
        String lastName = LAST_NAMES[Math.abs(random.nextInt(LAST_NAMES.length))];
        String name = firstName + " " + lastName;
        String avatar = avatarUrl(random);
        String profileUri = "https://api.github.com/users/" + login;
        String location = LOCATIONS[Math.abs(random.nextInt(LOCATIONS.length))];
        String company = firstName + " " + COMPANY_SUFFIX[Math.abs(random.nextInt(COMPANY_SUFFIX.length))];
        String blog = "https://" + login + ".example.com";
        String email = login + "@example.com";
        String bio = "Enthusiast of " + LANGUAGES[Math.abs(random.nextInt(LANGUAGES.length))] + " and "
                + TOPICS[Math.abs(random.nextInt(TOPICS.length))] + ".";
        int publicRepos = 20 + Math.abs(random.nextInt(30));
        int publicGists = Math.abs(random.nextInt(5));
        int followers = 50 + Math.abs(random.nextInt(200));
        int following = 10 + Math.abs(random.nextInt(80));
        Calendar created = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        created.add(Calendar.DAY_OF_YEAR, -(100 + Math.abs(random.nextInt(1500))));
        return new GitHubUserProfileDataEntry(name, avatar, profileUri, location, login,
                company, blog, email, bio, publicRepos, publicGists, followers, following, created);
    }

    private static MockRepository ensureRepository(String owner, String repoName, Random random) {
        String key = owner + "/" + repoName;
        return REPOSITORY_CACHE.computeIfAbsent(key, k -> new MockRepository(owner, repoName, random));
    }

    private static List<MockContribution> getContributionEntries(String login, int page) {
        String key = login + "-" + page;
        return CONTRIBUTION_CACHE.computeIfAbsent(key, k -> generateContributions(login, page));
    }

    private static List<MockContribution> generateContributions(String login, int page) {
        List<MockContribution> list = new ArrayList<>();
        if (page > 3) {
            return list;
        }
        Random random = randomFor("contrib-" + login + "-" + page);
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            ActionType actionType = pickContributionAction(random);
            String eventType = mapActionToEvent(actionType);
            long id = buildStableId(page, i + 100);
            String repoName = randomRepoName(random, page, i);
            String repoFullName = login + "/" + repoName;
            String repoUrl = "https://github.com/" + repoFullName;
            String commit = repoUrl + "/commit/" + generateHexToken(random, 8);
            Calendar date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            date.add(Calendar.HOUR_OF_DAY, -(page * 8 + i));
            String description = repoName + ", full path: " + repoUrl;
            ContributionDataEntry entry = new ContributionDataEntry(id, commit, eventType, description, actionType, date);
            list.add(new MockContribution(Long.toString(id), eventType, repoName, repoUrl, commit, entry));
        }
        return list;
    }

    private static long buildStableId(int page, int index) {
        return page * 10_000L + index * 37L;
    }

    private static Random randomFor(String seed) {
        return new Random(seed.hashCode());
    }

    private static String randomLogin(Random random) {
        String first = FIRST_NAMES[Math.abs(random.nextInt(FIRST_NAMES.length))];
        String last = LAST_NAMES[Math.abs(random.nextInt(LAST_NAMES.length))];
        return (first + last + Math.abs(random.nextInt(100))).toLowerCase(Locale.US);
    }

    private static String randomRepoName(Random random, int page, int index) {
        String topic = TOPICS[Math.abs(random.nextInt(TOPICS.length))];
        String suffix = Integer.toString(page) + Integer.toString(index);
        return (topic + "-" + suffix).toLowerCase(Locale.US);
    }

    private static String randomPersonName(Random random) {
        String first = FIRST_NAMES[Math.abs(random.nextInt(FIRST_NAMES.length))];
        String last = LAST_NAMES[Math.abs(random.nextInt(LAST_NAMES.length))];
        return first + " " + last;
    }

    private static String avatarUrl(Random random) {
        int id = 100 + Math.abs(random.nextInt(9_000));
        return "https://avatars.githubusercontent.com/u/" + id + "?v=4";
    }

    private static ActionType pickFeedAction(Random random) {
        ActionType[] values = {ActionType.PUSH, ActionType.PULL_REQUEST, ActionType.ISSUE, ActionType.STAR, ActionType.FORK};
        return values[Math.abs(random.nextInt(values.length))];
    }

    private static ActionType pickContributionAction(Random random) {
        ActionType[] values = {ActionType.PUSH, ActionType.PULL_REQUEST, ActionType.ISSUE, ActionType.RELEASE};
        return values[Math.abs(random.nextInt(values.length))];
    }

    private static String mapActionToEvent(ActionType actionType) {
        switch (actionType) {
            case PUSH:
                return "PushEvent";
            case PULL_REQUEST:
                return "PullRequestEvent";
            case ISSUE:
                return "IssuesEvent";
            case RELEASE:
                return "ReleaseEvent";
            case STAR:
                return "WatchEvent";
            case FORK:
                return "ForkEvent";
            default:
                return actionType.name() + "Event";
        }
    }

    private static String generateHexToken(Random random, int length) {
        StringBuilder builder = new StringBuilder(length);
        String alphabet = "0123456789abcdef";
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(Math.abs(random.nextInt(alphabet.length()))));
        }
        return builder.toString();
    }

    private static String formatIso(Calendar calendar) {
        if (calendar == null) {
            calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        }
        SimpleDateFormat format = new SimpleDateFormat(ISO_PATTERN, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(calendar.getTime());
    }

    private static class MockRepository {
        private final String owner;
        private final String name;
        private final String description;
        private final String language;
        private final boolean isPrivate;
        private final int stars;
        private final int forks;
        private final int watchers;
        private final Map<String, List<RepositoryContentDataEntry>> tree = new ConcurrentHashMap<>();
        private final Map<String, String> fileContents = new ConcurrentHashMap<>();

        MockRepository(String owner, String name, Random random) {
            this.owner = owner;
            this.name = name;
            this.description = "Mock repository for " + name + " created to showcase UI data.";
            this.language = LANGUAGES[Math.abs(random.nextInt(LANGUAGES.length))];
            this.isPrivate = random.nextBoolean();
            this.stars = 25 + Math.abs(random.nextInt(300));
            this.forks = 5 + Math.abs(random.nextInt(120));
            this.watchers = 10 + Math.abs(random.nextInt(200));
            buildTree();
        }

        JSONObject toJson() throws JSONException {
            JSONObject repo = new JSONObject();
            repo.put("name", name);
            repo.put("full_name", owner + "/" + name);
            repo.put("description", description);
            repo.put("language", language);
            repo.put("private", isPrivate);
            repo.put("fork", false);
            repo.put("forks_count", forks);
            repo.put("watchers_count", watchers);
            repo.put("stargazers_count", stars);
            JSONObject ownerJson = new JSONObject();
            ownerJson.put("login", owner);
            repo.put("owner", ownerJson);
            return repo;
        }

        JSONObject toStarredJson() throws JSONException {
            JSONObject repo = toJson();
            repo.put("full_name", owner + "/" + name);
            repo.put("private", false);
            return repo;
        }

        List<RepositoryContentDataEntry> getEntries(String path) {
            String safePath = path == null ? "" : path;
            List<RepositoryContentDataEntry> entries = tree.get(safePath);
            if (entries == null) {
                return Collections.emptyList();
            }
            return entries;
        }

        String getFileContent(String path) {
            return fileContents.get(path == null ? "" : path);
        }

        String buildDownloadUrl(String path) {
            Uri.Builder builder = new Uri.Builder()
                    .scheme("https")
                    .authority(MOCK_HOST)
                    .appendPath("repos")
                    .appendPath(owner)
                    .appendPath(name)
                    .appendPath("raw");
            if (path != null && !path.isEmpty()) {
                String[] segments = path.split("/");
                for (String segment : segments) {
                    builder.appendPath(segment);
                }
            }
            return builder.build().toString();
        }

        private void buildTree() {
            List<RepositoryContentDataEntry> root = new ArrayList<>();
            root.add(createFileEntry("README.md", "README.md", generateReadme()));
            root.add(createDirectoryEntry("src", "src"));
            root.add(createDirectoryEntry("docs", "docs"));
            tree.put("", root);

            List<RepositoryContentDataEntry> src = new ArrayList<>();
            src.add(createFileEntry("MainActivity.java", "src/MainActivity.java",
                    "package com.example;\n\n// Mock main activity for " + name + "\npublic class MainActivity {\n    public void run() {\n        System.out.println(\"Hello from " + name + "!\");\n    }\n}\n"));
            src.add(createFileEntry("Utils.kt", "src/Utils.kt",
                    "package com.example\n\nobject Utils {\n    fun random(): Int = (0..42).random()\n}\n"));
            tree.put("src", src);

            List<RepositoryContentDataEntry> docs = new ArrayList<>();
            docs.add(createFileEntry("overview.md", "docs/overview.md",
                    "# Overview\n\nThis module describes the architecture of **" + name + "**."));
            tree.put("docs", docs);
        }

        private RepositoryContentDataEntry createDirectoryEntry(String name, String path) {
            return new RepositoryContentDataEntry(name, "", GitHubRepoContentType.DIR, path);
        }

        private RepositoryContentDataEntry createFileEntry(String name, String path, String content) {
            String download = buildDownloadUrl(path);
            fileContents.put(path, content);
            return new RepositoryContentDataEntry(name, download, GitHubRepoContentType.FILE, path);
        }

        private String generateReadme() {
            return "# " + name + "\n\n" + description + "\n\n## Getting Started\n\n" +
                    "This project demonstrates mocked GitHub data for the GitJourney app.";
        }
    }

    private static class MockContribution {
        private final String id;
        private final String eventType;
        private final String repoName;
        private final String repoUrl;
        private final String commitUrl;
        private final ContributionDataEntry dataEntry;

        MockContribution(String id, String eventType, String repoName, String repoUrl, String commitUrl,
                         ContributionDataEntry dataEntry) {
            this.id = id;
            this.eventType = eventType;
            this.repoName = repoName;
            this.repoUrl = repoUrl;
            this.commitUrl = commitUrl;
            this.dataEntry = dataEntry;
        }

        JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("type", eventType);
            JSONObject repo = new JSONObject();
            repo.put("name", repoName);
            repo.put("url", repoUrl);
            obj.put("repo", repo);
            JSONObject payload = new JSONObject();
            JSONArray commits = new JSONArray();
            JSONObject commit = new JSONObject();
            commit.put("url", commitUrl);
            commits.put(commit);
            payload.put("commits", commits);
            obj.put("payload", payload);
            obj.put("created_at", formatIso(dataEntry.getDate()));
            return obj;
        }
    }
}