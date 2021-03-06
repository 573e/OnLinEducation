package com.hzlei.eduservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hzlei.eduservice.entity.EduCourse;
import com.hzlei.eduservice.entity.EduCourseDescription;
import com.hzlei.eduservice.entity.frontvo.CourseFrontVo;
import com.hzlei.eduservice.entity.vo.CourseInfoVo;
import com.hzlei.eduservice.entity.vo.CoursePublishVo;
import com.hzlei.eduservice.mapper.EduCourseMapper;
import com.hzlei.eduservice.service.EduChapterService;
import com.hzlei.eduservice.service.EduCourseDescriptionService;
import com.hzlei.eduservice.service.EduCourseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzlei.eduservice.service.EduVideoService;
import com.hzlei.servicebase.exceptionhandler.HzleiException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 课程表, 存储课程的基本信息 服务实现类
 * </p>
 *
 * @author hzlei
 * @since 2020-07-05
 */
@Service
public class EduCourseServiceImpl extends ServiceImpl<EduCourseMapper, EduCourse> implements EduCourseService {

    @Autowired
    private EduCourseDescriptionService courseDescriptionService;
    @Autowired
    private EduVideoService videoService;
    @Autowired
    private EduChapterService chapterService;

    // 添加课程基本信息
    @Override
    public String saveCourseInfo(CourseInfoVo course) {
        // 1. 向课程表(edu_course)添加课程基本信息
        // 将 CourseInfoVo 对象转换成 EduCourse 对象
        EduCourse eduCourse = new EduCourse();
        BeanUtils.copyProperties(course, eduCourse);
        int insert = baseMapper.insert(eduCourse);
        if (insert <= 0) throw new HzleiException(20001, "添加课程信息失败");

        // 2. 向课程简介表(edu_course_description)添加课程简介
        EduCourseDescription courseDescription = new EduCourseDescription();
        courseDescription.setId(eduCourse.getId());
        courseDescription.setDescription(course.getDescription());
        courseDescriptionService.save(courseDescription);
        return eduCourse.getId();
    }

    /**
     *  查询课程基本信息
     * @param courseId 课程id
     * @return
     */
    @Override
    public CourseInfoVo getCourseInfo(String courseId) {
        // 定义返回的数据
        CourseInfoVo courseInfoVo = new CourseInfoVo();

        // 1. 查询课程表
        EduCourse eduCourse = baseMapper.selectById(courseId);
        // 2. 查询课程描述表
        EduCourseDescription courseDescription = courseDescriptionService.getById(courseId);

        BeanUtils.copyProperties(eduCourse, courseInfoVo);
        BeanUtils.copyProperties(courseDescription, courseInfoVo);

        return courseInfoVo;
    }

    /**
     * 修改课程信息
     * @param courseInfoVo 课程描述对象
     * @return
     */
    @Transactional // 事务
    @Override
    public void updateCourseInfo(CourseInfoVo courseInfoVo) {
        // 1. 修改课程表
        EduCourse eduCourse = new EduCourse();
        BeanUtils.copyProperties(courseInfoVo, eduCourse);
        int updateEducourse = baseMapper.updateById(eduCourse);

        // 2. 修改课程描述表
        EduCourseDescription eduCourseDescription = new EduCourseDescription();
        BeanUtils.copyProperties(courseInfoVo, eduCourseDescription);
        boolean updateEduCourseDesc = courseDescriptionService.updateById(eduCourseDescription);

        if (updateEducourse == 0 || updateEduCourseDesc == false)
            throw new HzleiException(20001, "修改课程信息失败");
    }

    /**
     * 根据课程id（courseId）查询课程最终确认信息
     * @param courseId 课程 id
     * @return
     */
    @Override
    public CoursePublishVo getPublishCourseInfo(String courseId) {
        // 调用 mapper
        CoursePublishVo publishCourseInfo = baseMapper.getPublishCourseInfo(courseId);
        return publishCourseInfo;
    }

    /**
     * 删除课程
     * @param courseId 课程 id
     */
    @Override
    public void removeCourse(String courseId) {
        // 1. 根据课程 id 删除小节
        videoService.removeVideoByCourseId(courseId);

        // 2. 根据课程 id 删除章节
        chapterService.removeChapterByCourseId(courseId);

        // 3. 根据课程 id 删除描述
        courseDescriptionService.removeById(courseId);

        // 4. 根据课程 id 删除课程本身
        int result = baseMapper.deleteById(courseId);

        if (result == 0) throw  new HzleiException(20001, "删除课程失败");
    }

    /**
     * 课程 条件查询带分页
     *
     * @param pageCourse
     * @param courseFrontVo
     * @return
     */
    @Override
    public Map<String, Object> getCourseFrontList(Page<EduCourse> pageCourse, CourseFrontVo courseFrontVo) {
        // 返回数据
        Map<String, Object> map = new HashMap<>();

        // 查询条件
        QueryWrapper<EduCourse> queryWrapper = new QueryWrapper<>();
        // 判断一级分类
        if (!StringUtils.isEmpty(courseFrontVo.getSubjectParentId())) {
            queryWrapper.eq("subject_parent_id", courseFrontVo.getSubjectParentId());
        }
        // 判断二级分类
        if (!StringUtils.isEmpty(courseFrontVo.getSubjectId())) {
            queryWrapper.eq("subject_id", courseFrontVo.getSubjectId());
        }
        // 销量排序
        if (!StringUtils.isEmpty(courseFrontVo.getBuyCountSort())) {
            queryWrapper.orderByDesc("buy_count");
        }
        // 时间排序
        if (!StringUtils.isEmpty(courseFrontVo.getGmtCreateSort())) {
            queryWrapper.orderByDesc("gmt_create");
        }
        // 价格排序
        if (!StringUtils.isEmpty(courseFrontVo.getPriceSort())) {
            queryWrapper.orderByDesc("price");
        }

        baseMapper.selectPage(pageCourse, queryWrapper);

        // 查询出来的讲师数据集合
        map.put("records", pageCourse.getRecords());
        // 当前页
        map.put("current", pageCourse.getCurrent());
        // 总页数
        map.put("pages", pageCourse.getPages());
        // 每页数据条数
        map.put("size", pageCourse.getSize());
        // 总数据条数
        map.put("total", pageCourse.getTotal());
        // 是否有下一页
        map.put("hasNext", pageCourse.hasNext());
        // 是否有上一页
        map.put("hasPrevious", pageCourse.hasPrevious());

        return map;
    }
}
