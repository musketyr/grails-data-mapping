/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.orm;

import org.grails.orm.hibernate.GrailsHibernateTemplate;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.impl.CriteriaImpl;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * A result list for Criteria list calls, which is aware of the totalCount for
 * the paged result.
 *
 * @author Siegfried Puchbauer
 * @since 1.0
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class PagedResultList extends grails.gorm.PagedResultList {

    private transient GrailsHibernateTemplate hibernateTemplate;
    private Criteria criteria;
    public PagedResultList(GrailsHibernateTemplate template, Criteria crit) {
        super(null);
        criteria = crit;
        resultList = criteria.list();
        hibernateTemplate = template;
    }

    @Override
    protected void initialize() {
        // noop, already initialized
    }

    @Override
    public int getTotalCount() {
        if (totalCount == Integer.MIN_VALUE) {
            totalCount = (Integer)hibernateTemplate.execute(new HibernateCallback<Object>() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    CriteriaImpl impl = (CriteriaImpl) criteria;
                    Criteria totalCriteria = session.createCriteria(impl.getEntityOrClassName());
                    hibernateTemplate.applySettings(totalCriteria);

                    Iterator iterator = impl.iterateExpressionEntries();
                    while (iterator.hasNext()) {
                        CriteriaImpl.CriterionEntry entry = (CriteriaImpl.CriterionEntry) iterator.next();
                        totalCriteria.add(entry.getCriterion());
                    }
                    Iterator subcriteriaIterator = impl.iterateSubcriteria();
                    while (subcriteriaIterator.hasNext()) {
                        CriteriaImpl.Subcriteria sub = (CriteriaImpl.Subcriteria) subcriteriaIterator.next();
                        totalCriteria.createAlias(sub.getPath(), sub.getAlias(), sub.getJoinType(), sub.getWithClause());
                    }
                    totalCriteria.setProjection(impl.getProjection());
                    totalCriteria.setProjection(Projections.rowCount());
                    return ((Number)totalCriteria.uniqueResult()).intValue();
                }
            });
        }
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {

        // find the total count if it hasn't been done yet so when this is deserialized
        // the null GrailsHibernateTemplate won't be an issue
        getTotalCount();

        out.defaultWriteObject();
    }
}
