package io.julius.chow.data.source.cache

import com.google.firebase.auth.FirebaseAuth
import io.julius.chow.data.model.*
import io.julius.chow.data.source.DataSource
import io.julius.chow.domain.Exception
import io.julius.chow.domain.Result
import io.julius.chow.domain.model.UserType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import javax.inject.Inject

class LocalDataSource @Inject constructor(private val appDAO: AppDAO) : DataSource {

    override fun isUserLoggedIn(): Result<Boolean> {
        // Check if a user is currently signed in
        val mAuth = FirebaseAuth.getInstance()
        val currentUser = mAuth.currentUser

        return if (currentUser == null) {
            // No logged in user
            Result.Success(false)
        } else {
            // User is logged in

            val user: UserEntity? = appDAO.getUser(currentUser.uid)
            if (user != null && user.profileComplete) {
                Result.Success(true)
            } else {
                val restaurant: RestaurantEntity? = appDAO.getRestaurant(currentUser.uid)
                if (restaurant != null && restaurant.profileComplete) {
                    Result.Success(true)
                } else {
                    Result.Success(false)
                }
            }
        }
    }

    override fun getCurrentLoggedAccount(): Any? {
        // NOTE: This elvis expression is not useless. Room would return null if there are no entries in a table.
        val currentUser: UserEntity? = appDAO.fetchCurrentUser()
        return currentUser ?: appDAO.fetchCurrentRestaurant()
    }

    override fun getCurrentLoggedAccountType(): Result<UserType> {
        return when (getCurrentLoggedAccount()) {
            is UserEntity -> Result.Success(UserType.CUSTOMER)
            is RestaurantEntity -> Result.Success(UserType.RESTAURANT)
            else -> Result.Failure(Exception.LocalDataNotFoundException)
        }
    }

    override suspend fun getCurrentUser(): Flowable<Result<UserEntity>> {
        return Flowable.create({
            appDAO.getCurrentUser().distinctUntilChanged().subscribe { userEntity ->
                if (userEntity == null) {
                    it.onError(Exception.LocalDataNotFoundException)
                } else {
                    it.onNext(Result.Success(userEntity))
                }
            }
        }, BackpressureStrategy.LATEST)
    }

    override suspend fun fetchCurrentUser(): UserEntity {
        return appDAO.fetchCurrentUser()
    }

    override suspend fun fetchRestaurants(): Flowable<Result<List<RestaurantEntity>>> =
    // Since we can't get Room to return our custom Result type, we get the normal data, and create a Flowable
        // to which we pass a successful list of data objects or an error and propagate down to the subscriber.
        Flowable.create({
            appDAO.getRestaurants().distinctUntilChanged().subscribe { restaurants ->
                if (restaurants == null) {
                    it.onError(Exception.LocalDataNotFoundException)
                } else {
                    it.onNext(Result.Success(restaurants))
                }
            }
        }, BackpressureStrategy.LATEST)

    override suspend fun saveUser(userEntity: UserEntity): Result<Boolean> {
        // The only time we will be calling this function is when we are saving a signed in user,
        // which at that point becomes the current user.
        userEntity.apply { isCurrentUser = true }
        val rowId: Long? = appDAO.saveUser(userEntity)
        return if (rowId == null) {
            Result.Failure(Exception.LocalDataException("Failed to save user"))
        } else {
            Result.Success(true)
        }
    }

    override suspend fun getCurrentRestaurant(): Flowable<Result<RestaurantEntity>> {
        return Flowable.create({
            appDAO.getCurrentRestaurant().distinctUntilChanged().subscribe { restaurantEntity ->
                if (restaurantEntity == null) {
                    it.onError(Exception.LocalDataNotFoundException)
                } else {
                    it.onNext(Result.Success(restaurantEntity))
                }
            }
        }, BackpressureStrategy.LATEST)
    }

    override suspend fun fetchCurrentRestaurant(): RestaurantEntity {
        return appDAO.fetchCurrentRestaurant()
    }

    override fun saveRestaurants(restaurantEntities: List<RestaurantEntity>) {
        restaurantEntities.forEach { appDAO.saveRestaurant(it) }
    }

    override suspend fun saveRestaurant(restaurantEntity: RestaurantEntity): Result<Boolean> {
        // The only time we will be calling this function is when we are saving a signed in restaurant,
        // which at that point becomes the current restaurant.
        restaurantEntity.apply { isCurrentRestaurant = true }
        val rowId: Long? = appDAO.saveRestaurant(restaurantEntity)
        return if (rowId == null) {
            Result.Failure(Exception.LocalDataException("Failed to save restaurant"))
        } else {
            Result.Success(true)
        }
    }

    override suspend fun fetchRestaurantMenu(restaurantId: String): Flowable<Result<List<FoodEntity>>> =
    // Since we can't get Room to return our custom Result type, we get the normal data, and create a Flowable
        // to which we pass a successful list of data objects or an error and propagate down to the subscriber.
        Flowable.create({
            appDAO.getRestaurantMenu(restaurantId).distinctUntilChanged().subscribe { menu ->
                if (menu == null) {
                    it.onError(Exception.LocalDataNotFoundException)
                } else {
                    it.onNext(Result.Success(menu))
                }
            }
        }, BackpressureStrategy.LATEST)

    override fun saveFood(foodEntities: List<FoodEntity>) {
        foodEntities.forEach { appDAO.saveFood(it) }
    }

    override suspend fun saveFood(foodEntity: FoodEntity): Result<FoodEntity> {
        val rowId: Long? = appDAO.saveFood(foodEntity)
        return if (rowId != null) {
            Result.Success(foodEntity)
        } else {
            Result.Failure(Exception.LocalDataException("Failed to save to local storage"))
        }
    }

    override suspend fun getMenu(category: String): Flowable<Result<List<FoodEntity>>> {
        return Flowable.create({
            appDAO.getMenu(category).distinctUntilChanged().subscribe { food ->
                if (food == null) {
                    it.onError(Exception.LocalDataNotFoundException)
                } else {
                    it.onNext(Result.Success(food))
                }
            }
        }, BackpressureStrategy.LATEST)
    }

    override suspend fun getOrders(): Flowable<Result<List<OrderEntity>>> {
        return Flowable.create({
            appDAO.getOrders().distinctUntilChanged().subscribe { orders ->
                if (orders == null) {
                    it.onError(Exception.LocalDataNotFoundException)
                } else {
                    it.onNext(Result.Success(orders))
                }
            }
        }, BackpressureStrategy.LATEST)
    }

    override suspend fun getRestaurantOrders(restaurantId: String): Flowable<Result<List<OrderEntity>>> {
        return Flowable.create({
            appDAO.getRestaurantOrders(restaurantId).distinctUntilChanged().subscribe { orders ->
                if (orders == null) {
                    it.onError(Exception.LocalDataNotFoundException)
                } else {
                    it.onNext(Result.Success(orders))
                }
            }
        }, BackpressureStrategy.LATEST)
    }

    override fun getOrder(id: String): OrderEntity? {
        return appDAO.getOrder(id)
    }

    override fun saveOrder(orderEntity: OrderEntity): Boolean {
        val rowId: Long? = appDAO.saveOrder(orderEntity)
        return rowId != null
    }

    override fun deleteOrder(orderEntity: OrderEntity) {
        appDAO.deleteOrder(orderEntity)
    }

    override suspend fun savePlacedOrder(placedOrder: PlacedOrderEntity): Boolean {
        val rowId: Long? = appDAO.savePlacedOrder(placedOrder)

        // Remove all orders from local db
        placedOrder.orders.forEach {
            appDAO.deleteOrder(it)
        }

        return rowId != null
    }
}