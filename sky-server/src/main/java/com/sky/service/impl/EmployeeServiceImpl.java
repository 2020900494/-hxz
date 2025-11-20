package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import com.sky.context.BaseContext;
import com.sky.constant.StatusConstant;
import com.sky.constant.PasswordConstant;
import com.sky.dto.EmployeeDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.util.DigestUtils;
import com.github.pagehelper.Page;

import java.time.LocalDateTime;


import java.time.LocalDateTime;
import java.util.List;

import static com.sky.constant.StatusConstant.ENABLE;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // TODO 后期需要进行md5加密，然后再进行比对
        password=DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }


    @Override
    public void save(EmployeeDTO employeeDTO) {

        Employee employee = new Employee();

        // 拷贝前端传来的属性
        BeanUtils.copyProperties(employeeDTO, employee);

        // 状态：启用
        employee.setStatus(StatusConstant.ENABLE);

        // 默认密码：123456（MD5 加密）
        employee.setPassword(
                DigestUtils.md5DigestAsHex(
                        PasswordConstant.DEFAULT_PASSWORD.getBytes()
                )
        );

        // 创建时间 / 修改时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        // ★ 关键：使用当前登录用户的id，而不是写死 10L
        Long currentId = BaseContext.getCurrentId();
        employee.setCreateUser(currentId);
        employee.setUpdateUser(currentId);

        // 插入数据库
        employeeMapper.insert(employee);
    }
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO){
        // 1. 开启分页，PageHelper.startPage固定用法
        PageHelper.startPage(employeePageQueryDTO.getPage(),
                employeePageQueryDTO.getPageSize());

        //声明一个变量 page类型是Page<Employee>，Page 不是你自己写的类，
        // 它来自 MyBatis-PageHelper 分页插件。可以把它理解成一个“增强版的 List”
        // 2. 执行分页查询，调用 employeeMapper 里的 pageQuery
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);

        long total = page.getTotal();            // 总记录数
        List<Employee> records = page.getResult(); // 当前页列表

        return new PageResult(total, records);   // 封装成 PageResult 返回

    }

    @Override
    public void startOrStop(Integer status, Long id) {

        // update employee set status = ? where id = ?

        Employee employee = new Employee();
        employee.setStatus(status);
        employee.setId(id);

        employeeMapper.update(employee);
    }

}
