/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for querying {@link Visit} entities by date range.
 */
public interface VisitRepository extends JpaRepository<Visit, Integer> {

	@Query("""
			SELECT new org.springframework.samples.petclinic.owner.UpcomingVisit(
			    o.id,
			    CONCAT(o.firstName, ' ', o.lastName),
			    p.name,
			    v.date,
			    v.description)
			FROM Owner o JOIN o.pets p JOIN p.visits v
			WHERE v.date >= :startDate AND v.date <= :endDate
			ORDER BY v.date ASC
			""")
	List<UpcomingVisit> findUpcomingVisits(@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	@Query("""
			SELECT new org.springframework.samples.petclinic.owner.UpcomingVisit(
			    o.id,
			    CONCAT(o.firstName, ' ', o.lastName),
			    p.name,
			    v.date,
			    v.description)
			FROM Owner o JOIN o.pets p JOIN p.visits v
			WHERE v.date >= :startDate AND v.date <= :endDate
			AND o.id = :ownerId
			ORDER BY v.date ASC
			""")
	List<UpcomingVisit> findUpcomingVisitsByOwnerId(@Param("ownerId") int ownerId,
			@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

}
