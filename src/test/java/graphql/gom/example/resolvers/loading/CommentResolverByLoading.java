package graphql.gom.example.resolvers.loading;

import graphql.gom.Batched;
import graphql.gom.Resolver;
import graphql.gom.Resolving;
import graphql.gom.example.entities.Article;
import graphql.gom.example.entities.Comment;
import graphql.gom.example.services.ArticleService;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Resolver("Comment")
public final class CommentResolverByLoading {

    private static final AtomicInteger GET_ARTICLE_CALL_COUNT = new AtomicInteger();

    @Batched
    @Resolving("article")
    public Map<Comment, Article> getArticle(Set<Comment> comments) {
        GET_ARTICLE_CALL_COUNT.incrementAndGet();
        return ArticleService.findManyByComments(comments);
    }

    public static int getArticleCallCount() {
        return GET_ARTICLE_CALL_COUNT.get();
    }

    public static void resetCounts() {
        GET_ARTICLE_CALL_COUNT.set(0);
    }

}