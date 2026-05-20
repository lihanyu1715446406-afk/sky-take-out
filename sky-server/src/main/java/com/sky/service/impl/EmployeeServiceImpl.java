package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
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

import java.time.LocalDateTime;
import java.util.List;

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
        // 对前端传过来的明文密码进行md5加密处理
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
    //新增员工
    //@param employeeDTO
    public void save(EmployeeDTO employeeDTO){
        System.out.println("当前线程的id："+Thread.currentThread().getId());
        Employee employee=new Employee();

        //对象属性拷贝
        BeanUtils.copyProperties(employeeDTO,employee);

        //设置账号状态，默认正常，1正常，0锁定
        employee.setStatus(StatusConstant.ENABLE);

        //设置密码，默认为123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        //设置当前记录的创建时间和修改时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        //TODO 后期需要改为当前用户登录的id
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.insert(employee);
    }

    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO){
        PageHelper.startPage(employeePageQueryDTO.getPage(),employeePageQueryDTO.getPageSize());

        Page<Employee> page=employeeMapper.pageQuery(employeePageQueryDTO);
        //我们最终要返回的是一个pageresult对象，所以我们要想办法将page对象转换成pageresult
        //由于pageresult由total和records组成
        long total=page.getTotal();
        List<Employee> records=page.getResult();
        return new PageResult(total,records);
    }

    //启用禁用员工账号，本质为根据id更改status字段
    @Override
    public void startOrStop(Integer status, Long id) {
        //update employee set status =? where id = ?
        //接着要在mapper中书写sql语句，为了通用性，写成动态sql。并且参数为对象更好
        Employee employee =new Employee();
        employee.setStatus(status);
        employee.setId(id);

        employeeMapper.update(employee);//爆红，需要alt回车去扩展该方法
    }


    public Employee getById(Long id) {
        //需要调用持久层
        Employee employee=employeeMapper.getById(id);
        employee.setPassword("****");//设置返回给前端的密码，增强安全性
        return employee;
    }


    public void update(EmployeeDTO employeeDTO) {
        //employeeMapper.update()
        //我们之前已经写过update方法，但是要的是employee对象
        //进行对象属性拷贝，从DTO拷贝到employee上面去
        Employee employee=new Employee();
        BeanUtils.copyProperties(employeeDTO,employee);
        //设置更新时间和更新人
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());
        //设置好之后调用Mapper的update方法修改相关属性
        employeeMapper.update(employee);
    }
}
