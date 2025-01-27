package com.ddfinv.controller;

public class Database {
}

/*

// database.rs

//! This module handle direct database communications. This is
//! in the form of using queries to update / add / remove data
//! from the database, and to perform retrievals of stored data
//! for local operations.

// Examples used:
// https://crates.io/crates/config
// https://api.rocket.rs/v0.4/rocket/config/struct.ConfigBuilder
// https://www.reddit.com/r/rust/comments/1akmv4j/whats_the_best_practice_for_configuration/
// imports items needed for defining external files for configuration/secrets
use config::{Config, File};
// imports the Path struct from the standard library path module
use log::debug;
use std::borrow::Cow;
use std::env;
use std::path::PathBuf;
//use std::task::Context;
// imports the Queryable trait from the mysql crate, prelude module
use mysql::prelude::*;
// imports all public items from the mysql crate
use mysql::{params, OptsBuilder, Pool, SslOpts};

// imports all public items from the firm_models module
use crate::firm_models::*;
// imports necessary errors from errors module
use crate::errors::{ApplicationError, DatabaseError};
//
// ********************************************
// database.rs module definitions begin here:
// ********************************************
//

/// Represents the MySQL database connection
///
///encapsulates the mySQL database connection pool, and
///related configuration / connection details that are needed
///to communicate with the remote database.
///
///# Fields
///
///* `pool: Pool` - A connection pool used to manage db connections
///
#[derive(Clone, Debug)]
pub struct MySqlDatabase {
    pool: Pool,
    // populate with db related
}

impl MySqlDatabase {
    /// Constructor function for the the MySqlDatabase implementation
    ///
    /// Creates/ implements a new MySqlDatabase object struct/
    /// instance of the database struct. It pulls connection
    /// secrets from a config.toml file, configures the connection
    /// and establishes the new connection pool.
    ///
    ///# Returns
    ///
    ///* 'Result<Self, ApplicationError>' - The result of new connection, either:
    ///     'Ok(MySqlDatabase)' - The new, successfully established db connection
    ///     'ApplicationError' - An error referencing the cause of the failure
    ///
    pub fn new() -> Result<Self, ApplicationError> {
        let (user, pw, host, port, db_name, cert_path) = if let Ok(env) = env::var("ENVIRONMENT") {
            // use match instead of if for both dock options
            match env.as_str() {
                "production" => {
                    // ??? use for remote on oceanDigital
                    (
                        // TODO: add more detailed handling use following closure as template:
                        // .map_err(|e| ApplicationError::VarNAMEHERE(format!("MSGCONTENT: {}", e)))?,
                        env::var("DB_USERNAME").map_err(|e| {
                            ApplicationError::ConfigError(format!("ERROR WITH DB_USERNAME: {}", e))
                        })?,
                        env::var("DB_PASSWORD").map_err(|e| {
                            ApplicationError::ConfigError(format!("ERROR WITH DB_PASSWORD: {}", e))
                        })?,
                        env::var("DB_HOST").map_err(|e| {
                            ApplicationError::ConfigError(format!("ERROR WITH DB_HOST: {}", e))
                        })?,
                        env::var("DB_PORT")
                            .map_err(|e| {
                                ApplicationError::ConfigError(format!("ERROR WITH DB_PORT: {}", e))
                            })?
                            .parse::<u16>()
                            .map_err(|e| {
                                ApplicationError::ConfigError(format!(
                                    "ERROR PARSING DB_PORT: {}",
                                    e
                                ))
                            })?,
                        env::var("DB_NAME").map_err(|e| {
                            ApplicationError::ConfigError(format!("ERROR WITH DB_NAME: {}", e))
                        })?, // isolate remote hosting,so can build local too
                        PathBuf::from("/usr/src/app/ca-certificate.crt"),
                    )
                }
                "local" => Self::local_connection_config()?, // local docker image build case
                _ => Self::local_connection_config()?,
            }
        } else {
            Self::local_connection_config()?
        };

        // debug!("Env: {:?}", env::var("ENVIRONMENT"));

        if cert_path.exists() {
            // troubleshoot cert contents empty?
            match std::fs::read_to_string(&cert_path) {
                Ok(content) => debug!(
                    "CERT CONTENT CHECK: {}",
                    content.chars().take(100).collect::<String>()
                ),
                Err(e) => debug!("ERROR READING CERT CONTENT {}", e),
            }
        } else {
            debug!("CERT FILE MISSING? CERT PATH: {:?}", cert_path);
            debug!("CURRENT DIR CONTENTS: {:?}", std::fs::read_dir(".")?);
        }

        let opts = match env::var("ENVIRONMENT")
            .unwrap_or_else(|_| "local".to_string())
            .as_str()
        {
            "production" => {
                let ssl_opts = SslOpts::default()
                    .with_root_cert_path(Some(Cow::Owned(cert_path)))
                    .with_danger_skip_domain_validation(true);

                OptsBuilder::new()
                    .user(Some(user))
                    .pass(Some(pw))
                    .ip_or_hostname(Some(host))
                    .tcp_port(port)
                    .db_name(Some(db_name))
                    .ssl_opts(Some(ssl_opts))
            }
            _ => {
                // Local development - no SSL needed
                OptsBuilder::new()
                    .user(Some(user))
                    .pass(Some(pw))
                    .ip_or_hostname(Some(host))
                    .tcp_port(port)
                    .db_name(Some(db_name))
            }
        };

        // old remote db connection opts
        // let ssl_opts = SslOpts::default()
        //     // https://blog.logrocket.com/using-cow-rust-efficient-memory-utilization/
        //     .with_root_cert_path(Some(Cow::Owned(cert_path))) // https://doc.rust-lang.org/nightly/alloc/borrow/enum.Cow.html
        //     //.with_danger_accept_invalid_certs(true)
        //     .with_danger_skip_domain_validation(true);
        //
        // let opts = OptsBuilder::new()
        //     .user(Some(user))
        //     .pass(Some(pw))
        //     .ip_or_hostname(Some(host))
        //     .tcp_port(port)
        //     .db_name(Some(db_name))
        //     .ssl_opts(Some(ssl_opts));
        //
        //debug!("Database options: {:?}", opts);

        let pool = Pool::new(opts).map_err(|e| ApplicationError::ConfigError(e.to_string()))?;

        debug!("Pool creation successful"); // for troubleshooting / logging
        let _conn = pool
            .get_conn()
            .map_err(|e| ApplicationError::ConfigError(e.to_string()))?;
        //
        debug!("DB connected successfully."); // for troubleshooting / logging

        Ok(MySqlDatabase { pool })
    }
    // split to remove need to set up twice
    fn local_connection_config(
    ) -> Result<(String, String, String, u16, String, PathBuf), ApplicationError> {
        // quick setup to branch between local docker/local cargo build options
        let path_config =
            PathBuf::from(env::var("CONFIG_PATH").unwrap_or_else(|_| "./config.toml".to_string()));
        let config = Config::builder()
            .add_source(File::from(path_config).required(true))
            .build()?;
        // exported to remove duplicate definition
        Ok((
            config.get_string("database.username")?,
            config.get_string("database.password")?,
            config.get_string("database.host")?,
            config.get_int("database.port")? as u16,
            config.get_string("database.name")?,
            PathBuf::from(config.get_string("database.ca_cert")?),
        ))
    }
}
/// definition for the interface that is used to manage
/// database based operations in the application
///
/// this trait contains the methods needed to manage all
/// of the operations that involve connecting to the remote
/// database, querying data, both as retrieval, and to
/// update or add values. the implementation will handle specifics.
///
/// The implementation of this trait for the MySqlDatabase, is a prime
/// example of function delegation in rust.
pub trait DatabaseManager: Send + Sync {
    /// init fn to create box clone of DatabaseManager implementation.
    fn clone_box(&self) -> Box<dyn DatabaseManager + Send + Sync>;
    /// init fn to initiate a new transaction
    fn begin_transaction(&mut self) -> Result<(), DatabaseError>;
    /// init fn to commit the changes / current transaction
    fn commit_transaction(&mut self) -> Result<(), DatabaseError>;
    /// init fn to back data from this transaction
    fn rollback_transaction(&mut self) -> Result<(), DatabaseError>;
    /// init fn to get all clients from the database.
    fn get_clients(&self) -> Result<Vec<Client>, DatabaseError>;
    fn get_client_by_user_account_id(
        &self,
        user_account_id: i64,
    ) -> Result<Option<Client>, DatabaseError>;
    /// init fn to save new client to database.
    fn new_client(&mut self, client: &Client) -> Result<(), DatabaseError>;
    /// init fn to update client instance in database
    fn update_client(&mut self, client: &Client) -> Result<(), DatabaseError>;
    /// init fn to remove client instance from database
    fn remove_client(&mut self, client: &Client) -> Result<(), DatabaseError>;

    /// init fn to save employee instance in database
    fn new_employee(&mut self, employee: &Employee) -> Result<(), DatabaseError>;
    /// init fn to update employee instance in database
    fn update_employee(&mut self, employee: &Employee) -> Result<(), DatabaseError>;
    /// init fn to remove employee instance from database
    fn remove_employee(&mut self, employee_id: i32) -> Result<(), DatabaseError>;
    /// init fn to retrieve all employees from database
    fn get_employees(&self) -> Result<Vec<Employee>, DatabaseError>;
    /// init fn to retrieve an employee from database
    fn get_employee(&self, employee_id: i32) -> Result<Option<Employee>, DatabaseError>;
    fn get_employee_by_user_account_id(
        &self,
        user_account_id: i64,
    ) -> Result<Option<Employee>, DatabaseError>;

    //
    fn register_user(
        &mut self,
        user: &UserAccount,
        hashed_password: &str,
    ) -> Result<(), DatabaseError>;
    //
    fn get_user_by_email(&self, email: &str) -> Result<Option<UserAccount>, DatabaseError>;
    fn get_user_hash(&self, email: &str) -> Result<Option<String>, DatabaseError>;
    fn update_user_role(&mut self, user_id: i64, new_role: Role) -> Result<(), DatabaseError>;
}

impl Clone for Box<dyn DatabaseManager> {
    /// remove employee instance from database
    ///
    /// # Arguments
    ///
    /// * `employee_id` - employee_id for this instance
    ///
    ///# Returns
    ///
    ///* 'Self' - Self, the cloned, boxed DatabaseManager
    ///
    fn clone(&self) -> Self {
        self.clone_box()
    }
}

impl DatabaseManager for MySqlDatabase {
    /// Creates a new boxed clone of the implemented DatabaseManager
    ///
    /// # Arguments
    ///
    /// * `&self` - reference to self, the implementation of DatabaseManager
    ///
    ///# Returns
    ///
    ///* 'Box<dyn DatabaseManager>' - a new/cloned, boxed DatabaseManager
    ///
    fn clone_box(&self) -> Box<dyn DatabaseManager + Send + Sync> {
        Box::new(self.clone())
    }

    /// initiates / begins new database transaction
    ///
    /// # Arguments
    ///
    /// * `&mut self` - mutable reference to MySql database instance
    ///
    ///# Returns
    ///
    ///* 'Result<(), DatabaseError> ' -
    ///     on success:
    ///         Ok(()) - the transaction successfully initiated
    ///     on fail:
    ///         DatabaseError::ConnectionError - if db connection fails to init
    ///         DatabaseError::TransactionError - if the transaction query fails to init
    ///
    ///# Errors
    ///
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::TransactionError' - failure to execute transaction related query on database
    ///
    fn begin_transaction(&mut self) -> Result<(), DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|e| DatabaseError::ConnectionError(e.to_string()))?;
        conn.query_drop("START TRANSACTION")
            .map_err(|e| DatabaseError::TransactionError(e.to_string()))
    }
    /// attempts to commit the transaction
    ///
    /// # Arguments
    ///
    /// * `&mut self` - mutable reference to MySql database instance
    ///
    /// # Returns
    ///
    /// 'clients' vector of `Client` objects on success
    ///
    ///# Returns
    ///
    ///* 'Result<(), DatabaseError> ' -
    ///     on success:
    ///     on fail:
    ///         ConnectionError with failure to get pool/establish connection
    ///         TransactionError on failure to commit changes for the transaction
    ///
    ///# Errors
    ///
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::TransactionError' - failure to execute transaction related query on database
    ///
    fn commit_transaction(&mut self) -> Result<(), DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|e| DatabaseError::ConnectionError(e.to_string()))?;
        conn.query_drop("COMMIT")
            .map_err(|e| DatabaseError::TransactionError(e.to_string()))
    }
    /// attempts to rollback the transaction
    ///
    /// # Arguments
    ///
    /// * `&mut self` - mutable reference to MySql database instance
    ///
    ///# Returns
    ///
    ///* 'Result<(), DatabaseError> ' -
    ///     on success:
    ///         Ok(()) when the transaction successfully is rolled back
    ///     on fail:
    ///         ConnectionError when the database connection cannot be established
    ///         TransactionError on failure to commit changes for the transaction
    ///# Errors
    ///
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::TransactionError' - failure to execute transaction related query on database
    ///
    fn rollback_transaction(&mut self) -> Result<(), DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|e| DatabaseError::ConnectionError(e.to_string()))?;
        conn.query_drop("ROLLBACK")
            .map_err(|e| DatabaseError::TransactionError(e.to_string()))
    }

    fn register_user(
        &mut self,
        user: &UserAccount,
        hashed_password: &str,
    ) -> Result<(), DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|err| DatabaseError::ConnectionError(err.to_string()))?;
        conn.exec_drop(
            "INSERT INTO UserAccounts (email, hashed_password, first_name, last_name, role) 
         VALUES (?, ?, ?, ?, ?)",
            (
                user.email.clone(),
                hashed_password,
                user.first_name.clone(),
                user.last_name.clone(),
                format!("{:?}", user.role),
            ),
        )
        .map_err(|err| DatabaseError::QueryError(err.to_string()))?;
        Ok(())
    }

    fn get_user_by_email(&self, email: &str) -> Result<Option<UserAccount>, DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|err| DatabaseError::ConnectionError(err.to_string()))?;
        conn.exec_first(
            "SELECT id, email, first_name, last_name, role FROM UserAccounts WHERE email = ?",
            (email,),
        )
        .map(|row: Option<(i64, String, String, String, String)>| {
            row.map(|(id, email, first_name, last_name, role)| {
                // Parse the role string into a Role enum
                let parsed_role = role.parse::<Role>().unwrap_or(Role::Restricted);

                UserAccount {
                    id,
                    email,
                    hashed_password: String::new(),
                    first_name,
                    last_name,
                    role: parsed_role,
                    permissions: vec![],
                }
            })
        })
        .map_err(|err| DatabaseError::QueryError(err.to_string()))
    }

    fn get_user_hash(&self, email: &str) -> Result<Option<String>, DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|err| DatabaseError::ConnectionError(err.to_string()))?;
        conn.exec_first(
            "SELECT hashed_password FROM UserAccounts WHERE email = ?",
            (email,),
        )
        .map_err(|err| DatabaseError::QueryError(err.to_string()))
    }

    fn update_user_role(&mut self, user_id: i64, new_role: Role) -> Result<(), DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|err| DatabaseError::ConnectionError(err.to_string()))?;
        conn.exec_drop(
            "UPDATE UserAccounts SET role = ? WHERE id = ?",
            (format!("{:?}", new_role), user_id),
        )
        .map_err(|err| DatabaseError::QueryError(err.to_string()))?;
        Ok(())
    }

    /// attempt to save employee instance in database
    ///
    /// # Arguments
    ///
    /// * `&mut self` - mutable reference to MySql database instance
    /// * `employee` - Employee instance to add to database
    ///
    ///# Returns
    ///
    ///* 'Result<(), DatabaseError> ' -
    ///     on success:
    ///         Ok(()) status update when employee successfully inserted
    ///     on fail:
    ///         ConnectionError when the database connection cannot be established
    ///         QueryError on failure to successfully process this insert into query for employee table
    ///
    /// # Errors
    ///
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::QueryError' - failure to execute query on the database
    ///
    /// return `DatabaseError` if employee fails to save
    fn new_employee(&mut self, employee: &Employee) -> Result<(), DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|err| DatabaseError::ConnectionError(err.to_string()))?;
        conn.exec_drop(
            "INSERT INTO Employees (employee_name, user_account_id) VALUES (?, ?)",
            (employee.employee_name.clone(), employee.user_account_id),
        )
        .map_err(|err| DatabaseError::QueryError(err.to_string()))?;
        Ok(())
    }

    fn new_client(&mut self, client: &Client) -> Result<(), DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|err| DatabaseError::ConnectionError(err.to_string()))?;
        conn.exec_drop(
        "INSERT INTO Clients (client_name, client_service, assigned_employee_id, user_account_id) 
         VALUES (?, ?, ?, ?)",
        (
            client.client_name.clone(),
            client.client_service,
            client.asn_employee_id,
            client.user_account_id,
        ),
    )
    .map_err(|err| DatabaseError::QueryError(err.to_string()))?;
        Ok(())
    }

    fn get_employee_by_user_account_id(
        &self,
        user_account_id: i64,
    ) -> Result<Option<Employee>, DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|err| DatabaseError::ConnectionError(err.to_string()))?;
        conn.exec_first(
        "SELECT employee_id, employee_name, user_account_id FROM Employees WHERE user_account_id = ?",
        (user_account_id,),
    )
    .map(|row: Option<(i32, String, i64)>| {
        row.map(|(employee_id, employee_name, user_account_id)| Employee {
            employee_id,
            employee_name,
            user_account_id,
        })
    })
    .map_err(|err| DatabaseError::QueryError(err.to_string()))
    }

    fn get_client_by_user_account_id(
        &self,
        user_account_id: i64,
    ) -> Result<Option<Client>, DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|err| DatabaseError::ConnectionError(err.to_string()))?;
        conn.exec_first(
            "SELECT client_id, client_name, client_service, assigned_employee_id, user_account_id 
         FROM Clients WHERE user_account_id = ?",
            (user_account_id,),
        )
        .map(|row: Option<(i32, String, i32, i32, i64)>| {
            row.map(
                |(
                    client_id,
                    client_name,
                    client_service,
                    assigned_employee_id,
                    user_account_id,
                )| Client {
                    client_id,
                    client_name,
                    client_service,
                    asn_employee_id: assigned_employee_id,
                    user_account_id,
                },
            )
        })
        .map_err(|err| DatabaseError::QueryError(err.to_string()))
    }

    /// attempt to get all clients from the database.
    ///
    /// # Arguments
    ///
    /// * `&self` - reference to (self) MySql database instance
    ///
    /// # Returns
    ///
    ///* 'Result<(), DatabaseError> ' -
    ///* 'clients' vector of `Client` objects on success
    ///     on success:
    ///         Ok(Vec<Client>)- vector of all client objects & Ok status update
    ///     on fail:
    ///         ConnectionError when the database connection cannot be established
    ///         QueryError on failure to successfully process this select query
    ///
    ///# Errors
    ///
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::QueryError' - failure to execute query on the database
    ///
    fn get_clients(&self) -> Result<Vec<Client>, DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|e| DatabaseError::ConnectionError(e.to_string()))?;
        conn.query_map(
        "SELECT client_id, client_name, client_service, assigned_employee_id, user_account_id FROM clients",
        |(client_id, client_name, client_service, assigned_employee_id, user_account_id)| {
            Client {
                client_id,
                client_name,
                client_service,
                asn_employee_id: assigned_employee_id,
                user_account_id,
            }
        },
    )
    .map_err(|e| DatabaseError::QueryError(e.to_string()))
    }
    /// attempt to save new client to database.
    ///
    /// # Arguments
    ///
    /// * `&mut self` - mutable reference to MySql database instance
    /// * `client` - Client object instance to save
    ///
    ///# Returns
    ///
    ///* 'Result<(), DatabaseError> ' -
    ///     on success:
    ///         Ok(()) status update when client successfully inserted
    ///     on fail:
    ///         ConnectionError when the database connection cannot be established
    ///         QueryError on failure to successfully process this insert into query for clients table
    ///
    ///# Errors
    ///
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::QueryError' - failure to execute query on the database
    ///

    /// return `DatabaseError` if client fails to save
    // implement inserting new client row into database
    // fn new_client(&mut self, client: &Client) -> Result<(), DatabaseError> {
    //     let mut conn = self
    //         .pool
    //         .get_conn()
    //         .map_err(|e| DatabaseError::ConnectionError(e.to_string()))?;
    //     conn.exec_drop(
    //         "INSERT INTO clients (client_name, client_service, assigned_employee) VALUES (?, ?, ?)",
    //         (
    //             client.get_client_name(),
    //             client.get_client_service(),
    //             client.get_asn_employee(),
    //         ),
    //     )
    //     .map_err(|e| DatabaseError::QueryError(e.to_string()))
    // }
    /// attempt to update client instance in database
    ///
    /// # Arguments
    ///
    /// * `&mut self` - mutable reference to MySql database instance
    /// * `client` - the updated Client instance
    ///
    ///# Returns
    ///
    ///* 'Result<(), DatabaseError> ' -
    ///     on success:
    ///         Ok(()) status update when client successfully updated
    ///     on fail:
    ///         ConnectionError when the database connection cannot be established
    ///         QueryError on failure to successfully process this update query for clients table
    ///
    /// # Errors
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::QueryError' - failure to execute query on the database
    ///
    /// return `DatabaseError` if client update fails
    // implement updating a client row (service / employee partner) in db
    fn update_client(&mut self, client: &Client) -> Result<(), DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|e| DatabaseError::ConnectionError(e.to_string()))?;
        conn.exec_drop(
            "UPDATE clients SET client_name = ?, client_service = ?, assigned_employee = ? WHERE client_id = ?",
            (client.get_client_name(), client.get_client_service(), client.get_asn_employee(), client.get_client_id()),
        ).map_err(|e| DatabaseError::QueryError(e.to_string()))
    }
    /// attempt to remove client instance from database
    ///
    /// # Arguments
    ///
    /// * `client` - The `Client` instance to remove
    ///
    ///# Returns
    ///
    ///* 'Result<(), DatabaseError> ' -
    ///     on success:
    ///         Ok(()) status update when client successfully removed
    ///     on fail:
    ///         ConnectionError when the database connection cannot be established
    ///         QueryError on failure to successfully process this delete from query for clients table
    ///
    /// # Errors
    ///
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::QueryError' - failure to execute query on the database
    ///
    /// return `DatabaseError` if client removal fails
    // implement function to remove a client row from the db
    fn remove_client(&mut self, client: &Client) -> Result<(), DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|e| DatabaseError::ConnectionError(e.to_string()))?;
        conn.exec_drop(
            "DELETE FROM clients WHERE client_id = ?",
            (client.get_client_id(),),
        )
        .map_err(|e| DatabaseError::QueryError(e.to_string()))
    }

    /// attempt to get an employee from the database.
    ///
    /// # Arguments
    ///
    /// * `&self` - reference to (self) MySql database instance
    ///
    /// # Returns
    ///
    ///* 'Result<(), DatabaseError> ' - Optional Employee object, Error
    ///* 'employee' vector of `Employee` objects on success
    ///     on success:
    ///         Ok(Some(Employee object)) - the located employee object from db
    ///         Ok(None) - No employee object match found in db
    ///     on fail:
    ///         ConnectionError when the database connection cannot be established
    ///         QueryError on failure to successfully process this select query
    ///
    ///# Errors
    ///
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::QueryError' - failure to execute query on the database
    ///
    fn get_employee(&self, employee_id: i32) -> Result<Option<Employee>, DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|e| DatabaseError::ConnectionError(e.to_string()))?;
        let result: Option<(i32, String, i64)> = conn
        .exec_first(
            "SELECT employee_id, employee_name, user_account_id FROM employees WHERE employee_id = :id",
            params! {"id" => employee_id},
        )
        .map_err(|e| DatabaseError::QueryError(e.to_string()))?;

        match result {
            Some((id, name, user_account_id)) => {
                Ok(Some(Employee::new(id, &name, user_account_id)))
            }
            None => Ok(None),
        }
    }

    /// attempt to get all employees from the database.
    ///
    /// # Arguments
    ///
    /// * `&self` - reference to (self) MySql database instance
    ///
    /// # Returns
    ///
    ///* 'Result<(), DatabaseError> ' -
    ///* 'employees' vector of `Employee` objects on success
    ///     on success:
    ///         Ok(Vec<Employee>)- vector of all employee objects & Ok status update
    ///     on fail:
    ///         ConnectionError when the database connection cannot be established
    ///         QueryError on failure to successfully process this select query
    ///
    ///# Errors
    ///
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::QueryError' - failure to execute query on the database
    ///
    fn get_employees(&self) -> Result<Vec<Employee>, DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|e| DatabaseError::ConnectionError(e.to_string()))?;
        let employee_data: Vec<(i32, String, i64)> = conn
            .query_map(
                "SELECT employee_id, employee_name, user_account_id FROM employees",
                |(employee_id, employee_name, user_account_id)| {
                    (employee_id, employee_name, user_account_id)
                },
            )
            .map_err(|e| DatabaseError::QueryError(e.to_string()))?;

        Ok(employee_data
            .into_iter()
            .map(|(id, name, user_account_id)| Employee::new(id, &name, user_account_id))
            .collect())
    }

    /// attempt to retrieve employee pass_hash from database
    ///
    /// # Arguments
    ///
    /// * `&mut self` - mutable reference to MySql database instance
    /// * `employee_id` - employee_id for this instance
    ///
    ///# Returns
    ///
    ///* 'Result<(), DatabaseError> ' -
    ///     on success:
    ///         Ok(()) status update when client successfully inserted
    ///     on fail:
    ///         ConnectionError when the database connection cannot be established
    ///         QueryError on failure to successfully process this insert into query for clients table
    ///
    ///  `Option<String>` with pass_hash on success
    ///
    /// # Errors
    ///
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::QueryError' - failure to execute query on the database
    ///
    /// return `DatabaseError` if failure to access database / could not find.
    // query database for the employee with ID
    // return their password hash if found, or None if not found
    // fn get_employee_hash(&mut self, employee_id: i32) -> Result<Option<String>, DatabaseError> {
    //     let mut conn = self
    //         .pool
    //         .get_conn()
    //         .map_err(|e| DatabaseError::ConnectionError(e.to_string()))?;
    //     let result: Option<String> = conn
    //         .exec_first(
    //             "SELECT hashed_password FROM employees WHERE employee_id = :id",
    //             params! {
    //             "id" => employee_id
    //             },
    //         )
    //         .map_err(|e| DatabaseError::QueryError(e.to_string()))?;
    //     Ok(result)
    // }

    /// attempt to update employee instance in database
    ///
    /// # Arguments
    ///
    /// * `&mut self` - mutable reference to MySql database instance
    /// * `employee` - Employee instance to update in db
    ///
    ///# Returns
    ///
    ///* 'Result<(), DatabaseError> ' -
    ///     on success:
    ///         Ok(()) status update when employee successfully updated
    ///     on fail:
    ///         ConnectionError when the database connection cannot be established
    ///         QueryError on failure to successfully process this update query for employee table
    ///
    /// # Errors
    ///
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::QueryError' - failure to execute query on the database
    ///
    /// return `DatabaseError` if employee fails to update in db
    fn update_employee(&mut self, employee: &Employee) -> Result<(), DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|e| DatabaseError::ConnectionError(e.to_string()))?;
        conn.exec_drop(
            "UPDATE employees SET employee_name = :name WHERE employee_id = :id",
            params! {
                "name" => employee.employee_name.clone(), // Clone the string
                "id" => employee.employee_id,
            },
        )
        .map_err(|e| DatabaseError::QueryError(e.to_string()))?;
        Ok(())
    }

    /// attempt to remove employee instance from database
    ///
    /// # Arguments
    ///
    /// * `&mut self` - mutable reference to MySql database instance
    /// * `employee_id` - employee_id for this instance
    ///
    ///# Returns
    ///
    ///* 'Result<(), DatabaseError> ' -
    ///     on success:
    ///         Ok(()) status update when employee successfully removed
    ///     on fail:
    ///         ConnectionError when the database connection cannot be established
    ///         QueryError on failure to successfully process this delete from query for employee table
    ///
    /// # Errors
    ///
    ///* 'DatabaseError::ConnectionError' - failure to establish connection to the database
    ///* 'DatabaseError::QueryError' - failure to execute query on the database
    ///
    fn remove_employee(&mut self, employee_id: i32) -> Result<(), DatabaseError> {
        let mut conn = self
            .pool
            .get_conn()
            .map_err(|e| DatabaseError::ConnectionError(e.to_string()))?;
        conn.exec_drop(
            "DELETE FROM employees WHERE employee_id = :id",
            params! {
                "id" => employee_id,
            },
        )
        .map_err(|e| DatabaseError::QueryError(e.to_string()))?;
        Ok(())
    }
}


 */