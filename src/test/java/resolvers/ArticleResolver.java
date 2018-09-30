package resolvers;

import db.Article;
import db.Blog;
import db.Comment;
import db.Repository;
import graphql.gom.GomBatched;
import graphql.gom.GomResolver;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

@GomResolver("Article")
public final class ArticleResolver {

    public static final ArticleResolver INSTANCE = new ArticleResolver();

    private ArticleResolver() {
    }

    @GomBatched
    public Mono<Map<Article, Blog>> blog(Set<Article> articles) {
        return Repository
                .findAllBlogsByIds(articles.stream().map(article -> article.blog.id).collect(toSet()))
                .collect(toMap(blog -> blog.id, identity()))
                .map(blogsById -> articles
                        .stream()
                        .collect(toMap(identity(), article -> blogsById.get(article.blog.id)))
                );
    }

    @GomBatched
    public Mono<Map<Article, List<Comment>>> comments(Set<Article> articles) {
        return Repository
                .findAllCommentsByArticleIds(articles.stream().map(article -> article.id).collect(toSet()))
                .collect(groupingBy(comment -> comment.article));
    }

}