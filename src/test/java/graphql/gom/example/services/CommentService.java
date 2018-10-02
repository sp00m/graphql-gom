package graphql.gom.example.services;

import graphql.gom.example.entities.Article;
import graphql.gom.example.entities.Comment;
import graphql.gom.example.entities.Entity;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static graphql.gom.example.entities.Database.COMMENTS_BY_ID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class CommentService {

    public static List<Comment> findManyByArticle(Article article, Optional<String> containing) {
        return COMMENTS_BY_ID
                .values()
                .stream()
                .filter(comment -> comment.getArticle().getId() == article.getId())
                .filter(comment -> containing.map(needle -> comment.getContent().contains(needle)).orElse(true))
                .collect(toList());
    }

    public static Map<Article, List<Comment>> findManyByArticles(Set<Article> articles, Optional<String> containing) {
        Map<Integer, Article> articlesById = articles
                .stream()
                .collect(toMap(Entity::getId, identity()));
        Set<Integer> articleIds = articlesById.keySet();
        return COMMENTS_BY_ID
                .values()
                .stream()
                .filter(comment -> articleIds.contains(comment.getArticle().getId()))
                .filter(comment -> containing.map(needle -> comment.getContent().contains(needle)).orElse(true))
                .collect(groupingBy(comment -> articlesById.get(comment.getArticle().getId())));
    }

}
