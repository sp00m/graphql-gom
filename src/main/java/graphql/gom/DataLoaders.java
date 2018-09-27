package graphql.gom;

import example.db.Article;
import example.db.Blog;
import example.db.Comment;
import graphql.gom.batching.DataLoaderKey;
import org.dataloader.DataLoader;
import example.resolvers.ArticleResolver;
import example.resolvers.BlogResolver;
import example.resolvers.CommentResolver;
import graphql.gom.utils.FutureParalleliser;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static org.dataloader.DataLoader.newMappedDataLoader;

public final class DataLoaders {

    public static DataLoader<Article, Blog> articleToBlogBatchLoader() {
        return newMappedDataLoader(articles -> ArticleResolver
                .INSTANCE
                .getBlog(articles)
                .toFuture());
    }

    public static DataLoader<Comment, Article> commentToArticleBatchLoader() {
        return newMappedDataLoader(comments -> CommentResolver
                .INSTANCE
                .getArticle(comments)
                .toFuture());
    }

    public static DataLoader<DataLoaderKey<Blog>, List<Article>> blogToArticlesBatchLoader() {
        return newMappedDataLoader(blogKeys -> {
            Map<Map<String, Object>, List<DataLoaderKey<Blog>>> keysByArguments = blogKeys.stream().collect(groupingBy(DataLoaderKey::getArguments));
            List<CompletableFuture<Map<DataLoaderKey<Blog>, List<Article>>>> futures = keysByArguments
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        Map<Blog, DataLoaderKey<Blog>> keysByBlog = entry.getValue().stream().collect(toMap(DataLoaderKey::getSource, identity()));
                        Set<Blog> blogs = keysByBlog.keySet();
                        Optional<String> maybeTitle = Optional.ofNullable((String) entry.getKey().get("title"));
                        return BlogResolver
                                .INSTANCE
                                .getArticles(blogs, maybeTitle)
                                .toFuture()
                                .thenApply(articlesByBlog -> articlesByBlog
                                        .entrySet()
                                        .stream()
                                        .collect(toMap(e -> keysByBlog.get(e.getKey()), Map.Entry::getValue))
                                );
                    })
                    .collect(toList());
            return FutureParalleliser
                    .parallelise(futures)
                    .thenApply(results -> results
                            .stream()
                            .reduce((r1, r2) -> {
                                Map<DataLoaderKey<Blog>, List<Article>> merged = new HashMap<>();
                                merged.putAll(r1);
                                merged.putAll(r2);
                                return merged;
                            })
                            .orElseGet(Collections::emptyMap)
                    );
        });
    }

    public static DataLoader<Article, List<Comment>> articleToCommentsBatchLoader() {
        return newMappedDataLoader(articles -> ArticleResolver
                .INSTANCE
                .getComments(articles)
                .toFuture());
    }

}
